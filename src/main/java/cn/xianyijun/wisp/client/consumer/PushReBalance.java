package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;

/**
 * @author xianyijun
 */
public class PushReBalance extends AbstractReBalance{

    private final ConsumerPushDelegate pushDelegate;

    public PushReBalance(ConsumerPushDelegate pushDelegate) {
        this(null, null, null, null, pushDelegate);
    }

    public PushReBalance(String consumerGroup, MessageModel messageModel,
                             AllocateMessageQueueStrategy allocateMessageQueueStrategy,
                             ClientInstance mQClientFactory, ConsumerPushDelegate pushDelegate) {
        super(consumerGroup, messageModel, allocateMessageQueueStrategy, mQClientFactory);
        this.pushDelegate = pushDelegate;
    }

    @Override
    public boolean removeUnnecessaryMessageQueue(MessageQueue mq, ProcessQueue pq) {
        return false;
    }
}
