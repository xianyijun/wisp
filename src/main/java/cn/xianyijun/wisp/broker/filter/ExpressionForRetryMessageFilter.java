package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.filter.ExpressionType;
import cn.xianyijun.wisp.filter.MessageEvaluationContext;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author xianyijun
 */
@Slf4j
public class ExpressionForRetryMessageFilter extends ExpressionMessageFilter {
    public ExpressionForRetryMessageFilter(SubscriptionData subscriptionData, ConsumerFilterData consumerFilterData,
                                           ConsumerFilterManager consumerFilterManager) {
        super(subscriptionData, consumerFilterData, consumerFilterManager);
    }

    @Override
    public boolean isMatchedByCommitLog(ByteBuffer msgBuffer, Map<String, String> properties) {
        if (subscriptionData == null) {
            return true;
        }

        if (subscriptionData.isClassFilterMode()) {
            return true;
        }

        boolean isRetryTopic = subscriptionData.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX);

        if (!isRetryTopic && ExpressionType.isTagType(subscriptionData.getExpressionType())) {
            return true;
        }

        ConsumerFilterData realFilterData = this.consumerFilterData;
        Map<String, String> tempProperties = properties;
        boolean decoded = false;
        if (isRetryTopic) {
            if (tempProperties == null && msgBuffer != null) {
                decoded = true;
                tempProperties = MessageDecoder.decodeProperties(msgBuffer);
            }
            String realTopic = tempProperties.get(MessageConst.PROPERTY_RETRY_TOPIC);
            String group = subscriptionData.getTopic().substring(MixAll.RETRY_GROUP_TOPIC_PREFIX.length());
            realFilterData = this.consumerFilterManager.get(realTopic, group);
        }

        // no expression
        if (realFilterData == null || realFilterData.getExpression() == null
                || realFilterData.getCompiledExpression() == null) {
            return true;
        }

        if (!decoded && tempProperties == null && msgBuffer != null) {
            tempProperties = MessageDecoder.decodeProperties(msgBuffer);
        }

        Object ret = null;
        try {
            MessageEvaluationContext context = new MessageEvaluationContext(tempProperties);

            ret = realFilterData.getCompiledExpression().evaluate(context);
        } catch (Throwable e) {
            log.error("Message Filters error, " + realFilterData + ", " + tempProperties, e);
        }

        log.debug("Pull eval result: {}, {}, {}", ret, realFilterData, tempProperties);

        if (ret == null || !(ret instanceof Boolean)) {
            return false;
        }
        return (Boolean) ret;
    }
}
