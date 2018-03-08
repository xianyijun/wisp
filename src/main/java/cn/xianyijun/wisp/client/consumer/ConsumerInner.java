package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;

import java.util.Set;

/**
 * The interface Mq consumer inner.
 */
public interface ConsumerInner {
    /**
     * Group name string.
     *
     * @return the string
     */
    String groupName();

    /**
     * Message model message model.
     *
     * @return the message model
     */
    MessageModel messageModel();

    /**
     * Consume type consume type.
     *
     * @return the consume type
     */
    ConsumeType consumeType();

    /**
     * Consume from where consume where enum.
     *
     * @return the consume where enum
     */
    ConsumeWhereEnum consumeFromWhere();

    /**
     * Subscriptions set.
     *
     * @return the set
     */
    Set<SubscriptionData> subscription();

    /**
     * Do rebalance.
     */
    void doReBalance();

    /**
     * Persist consumer offset.
     */
    void persistConsumerOffset();

    /**
     * Update topic subscribe info.
     *
     * @param topic the topic
     * @param info  the info
     */
    void updateTopicSubscribeInfo(final String topic, final Set<MessageQueue> info);

    /**
     * Is subscribe topic need update boolean.
     *
     * @param topic the topic
     * @return the boolean
     */
    boolean isSubscribeTopicNeedUpdate(final String topic);

    /**
     * Is unit mode boolean.
     *
     * @return the boolean
     */
    boolean isUnitMode();

    /**
     * Consumer running info consumer running info.
     *
     * @return the consumer running info
     */
    ConsumerRunningInfo consumerRunningInfo();
}