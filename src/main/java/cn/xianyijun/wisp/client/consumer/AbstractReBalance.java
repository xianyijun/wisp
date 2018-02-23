package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public abstract class AbstractReBalance {

    protected final ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = new ConcurrentHashMap<>(64);

    private final ClientInstance clientFactory;

    protected String consumerGroup;

    protected MessageModel messageModel;

    protected AllocateMessageQueueStrategy allocateMessageQueueStrategy;

    AbstractReBalance(String consumerGroup, MessageModel messageModel,
                      AllocateMessageQueueStrategy allocateMessageQueueStrategy,
                      ClientInstance clientFactory) {
        this.consumerGroup = consumerGroup;
        this.messageModel = messageModel;
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
        this.clientFactory = clientFactory;
    }
}
