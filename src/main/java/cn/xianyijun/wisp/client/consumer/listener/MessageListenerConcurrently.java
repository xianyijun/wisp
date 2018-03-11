package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.common.message.ExtMessage;

import java.util.List;

/**
 * The interface Message listener concurrently.
 *
 * @author xianyijun
 */
public interface MessageListenerConcurrently extends MessageListener {
    /**
     * Consume message consume concurrently status.
     *
     * @param msgs    the msgs
     * @param context the context
     * @return the consume concurrently status
     */
    ConsumeConcurrentlyStatus consumeMessage(final List<ExtMessage> msgs,
                                             final ConsumeConcurrentlyContext context);
}

