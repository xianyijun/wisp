package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;

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
}
