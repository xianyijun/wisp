package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type Abstract re balance.
 *
 * @author xianyijun
 */
@Slf4j
@Getter
public abstract class AbstractReBalance {

    /**
     * The Process queue table.
     */
    protected final ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = new ConcurrentHashMap<>(64);

    protected final ConcurrentMap<String /* topic */, SubscriptionData> subscriptionInner =
            new ConcurrentHashMap<>();

    @Setter
    private  ClientInstance clientFactory;

    /**
     * The Consumer group.
     */
    @Setter
    protected String consumerGroup;

    /**
     * The Message model.
     */
    @Setter
    protected MessageModel messageModel;

    /**
     * The Allocate message queue strategy.
     */
    @Setter
    protected AllocateMessageQueueStrategy allocateMessageQueueStrategy;

    /**
     * Instantiates a new Abstract re balance.
     *
     * @param consumerGroup                the consumer group
     * @param messageModel                 the message model
     * @param allocateMessageQueueStrategy the allocate message queue strategy
     * @param clientFactory                the client factory
     */
    AbstractReBalance(String consumerGroup, MessageModel messageModel,
                      AllocateMessageQueueStrategy allocateMessageQueueStrategy,
                      ClientInstance clientFactory) {
        this.consumerGroup = consumerGroup;
        this.messageModel = messageModel;
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
        this.clientFactory = clientFactory;
    }

    /**
     * Remove unnecessary message queue boolean.
     *
     * @param mq the mq
     * @param pq the pq
     * @return the boolean
     */
    public abstract boolean removeUnnecessaryMessageQueue(final MessageQueue mq, final ProcessQueue pq);

}
