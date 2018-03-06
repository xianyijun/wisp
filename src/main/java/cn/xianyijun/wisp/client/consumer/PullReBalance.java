package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;

/**
 * todo
 * @author xianyijun
 */
public class PullReBalance extends AbstractReBalance {

    private final ConsumerPullDelegate consumerPullDelegate;

    public PullReBalance(ConsumerPullDelegate consumerPullDelegate) {
        this(null, null, null, null, consumerPullDelegate);
    }

    public PullReBalance(String consumerGroup, MessageModel messageModel,
                             AllocateMessageQueueStrategy allocateMessageQueueStrategy,
                             ClientInstance clientFactory, ConsumerPullDelegate consumerPullDelegate) {
        super(consumerGroup, messageModel, allocateMessageQueueStrategy, clientFactory);
        this.consumerPullDelegate = consumerPullDelegate;
    }

    @Override
    public boolean removeUnnecessaryMessageQueue(MessageQueue mq, ProcessQueue pq) {
        return false;
    }
}
