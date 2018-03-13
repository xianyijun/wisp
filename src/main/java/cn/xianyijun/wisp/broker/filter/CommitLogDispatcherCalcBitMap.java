package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.filter.MessageEvaluationContext;
import cn.xianyijun.wisp.filter.support.BitsArray;
import cn.xianyijun.wisp.store.CommitLogDispatcher;
import cn.xianyijun.wisp.store.request.DispatchRequest;
import cn.xianyijun.wisp.utils.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class CommitLogDispatcherCalcBitMap implements CommitLogDispatcher {

    protected final BrokerConfig brokerConfig;
    protected final ConsumerFilterManager consumerFilterManager;


    @Override
    public void dispatch(DispatchRequest request) {
        if (!this.brokerConfig.isEnableCalcFilterBitMap()) {
            return;
        }

        Collection<ConsumerFilterData> filterDataCollection = consumerFilterManager.get(request.getTopic());

        if (CollectionUtils.isEmpty(filterDataCollection)) {
            return;
        }
        Iterator<ConsumerFilterData> iterator = Objects.requireNonNull(filterDataCollection).iterator();
        BitsArray filterBitMap = BitsArray.create(
                this.consumerFilterManager.getBloomFilter().getM()
        );
        long startTime = System.currentTimeMillis();

        while (iterator.hasNext()) {
            ConsumerFilterData filterData = iterator.next();

            if (filterData.getCompiledExpression() == null) {
                log.error("Consumer in filter manager has no compiled expression! {}", filterData);
                continue;
            }

            if (filterData.getBloomFilterData() == null) {
                log.error("Consumer in filter manager has no bloom data! {}", filterData);
                continue;
            }

            Object ret = null;
            try {
                MessageEvaluationContext context = new MessageEvaluationContext(request.getPropertiesMap());

                ret = filterData.getCompiledExpression().evaluate(context);
            } catch (Throwable e) {
                log.error("Calc filter bit map error!commitLogOffset={}, consumer={}, {}", request.getCommitLogOffset(), filterData, e);
            }

            log.debug("Result of Calc bit map:ret={}, data={}, props={}, offset={}", ret, filterData, request.getPropertiesMap(), request.getCommitLogOffset());

            if (ret != null && ret instanceof Boolean && (Boolean) ret) {
                consumerFilterManager.getBloomFilter().hashTo(
                        filterData.getBloomFilterData(),
                        filterBitMap
                );
            }
        }

        request.setBitMap(filterBitMap.getBytes());

        long eclipseTime = System.currentTimeMillis() - startTime;
        // 1ms
        if (eclipseTime >= 1) {
            log.warn("Spend {} ms to calc bit map, consumerNum={}, topic={}", eclipseTime, filterDataCollection.size(), request.getTopic());
        }
    }
}
