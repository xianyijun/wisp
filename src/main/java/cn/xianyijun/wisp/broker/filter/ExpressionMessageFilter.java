package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.filter.ExpressionType;
import cn.xianyijun.wisp.filter.utils.BitsArray;
import cn.xianyijun.wisp.filter.utils.BloomFilter;
import cn.xianyijun.wisp.store.ConsumeQueueExt;
import cn.xianyijun.wisp.store.MessageFilter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ExpressionMessageFilter implements MessageFilter {

    protected final SubscriptionData subscriptionData;
    protected final ConsumerFilterData consumerFilterData;
    protected final ConsumerFilterManager consumerFilterManager;
    protected final boolean bloomDataValid;

    public ExpressionMessageFilter(SubscriptionData subscriptionData, ConsumerFilterData consumerFilterData,
                                   ConsumerFilterManager consumerFilterManager) {
        this.subscriptionData = subscriptionData;
        this.consumerFilterData = consumerFilterData;
        this.consumerFilterManager = consumerFilterManager;
        if (consumerFilterData == null) {
            bloomDataValid = false;
            return;
        }
        BloomFilter bloomFilter = this.consumerFilterManager.getBloomFilter();
        bloomDataValid = bloomFilter != null && bloomFilter.isValid(consumerFilterData.getBloomFilterData());
    }

    @Override
    public boolean isMatchedByConsumeQueue(Long tagsCode, ConsumeQueueExt.CqExtUnit cqExtUnit) {
        if (null == subscriptionData) {
            return true;
        }

        if (subscriptionData.isClassFilterMode()) {
            return true;
        }

        if (ExpressionType.isTagType(subscriptionData.getExpressionType())) {

            return tagsCode == null || subscriptionData.getSubString().equals(SubscriptionData.SUB_ALL) || subscriptionData.getCodeSet().contains(tagsCode.intValue());

        } else {
            // no expression or no bloom
            if (consumerFilterData == null || consumerFilterData.getExpression() == null
                    || consumerFilterData.getCompiledExpression() == null || consumerFilterData.getBloomFilterData() == null) {
                return true;
            }

            // message is before consumer
            if (cqExtUnit == null || !consumerFilterData.isMsgInLive(cqExtUnit.getMsgStoreTime())) {
                log.debug("Pull matched because not in live: {}, {}", consumerFilterData, cqExtUnit);
                return true;
            }

            byte[] filterBitMap = cqExtUnit.getFilterBitMap();
            BloomFilter bloomFilter = this.consumerFilterManager.getBloomFilter();
            if (filterBitMap == null || !this.bloomDataValid
                    || filterBitMap.length * Byte.SIZE != consumerFilterData.getBloomFilterData().getBitNum()) {
                return true;
            }

            BitsArray bitsArray = null;
            try {
                bitsArray = BitsArray.create(filterBitMap);
                boolean ret = bloomFilter.isHit(consumerFilterData.getBloomFilterData(), bitsArray);
                log.debug("Pull {} by bit map:{}, {}, {}", ret, consumerFilterData, bitsArray, cqExtUnit);
                return ret;
            } catch (Throwable e) {
                log.error("bloom filter error, sub=" + subscriptionData
                        + ", filter=" + consumerFilterData + ", bitMap=" + bitsArray, e);
            }
        }
        return true;
    }

    @Override
    public boolean isMatchedByCommitLog(ByteBuffer msgBuffer, Map<String, String> properties) {
        return false;
    }
}
