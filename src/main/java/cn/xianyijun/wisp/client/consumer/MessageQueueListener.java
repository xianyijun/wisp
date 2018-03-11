package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.MessageQueue;

import java.util.Set;

/**
 * The interface Message queue listener.
 */
public interface MessageQueueListener {

    /**
     * Message queue changed.
     *
     * @param topic     the topic
     * @param mqAll     the mq all
     * @param mqDivided the mq divided
     */
    void messageQueueChanged(final String topic, final Set<MessageQueue> mqAll,
                             final Set<MessageQueue> mqDivided);
}
