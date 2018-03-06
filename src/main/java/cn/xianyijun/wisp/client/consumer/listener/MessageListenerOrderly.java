package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.common.message.ExtMessage;

import java.util.List;

/**
 * The interface Message listener orderly.
 */
public interface MessageListenerOrderly extends MessageListener  {
    /**
     * Consume message consume orderly status.
     *
     * @param msgs    the msgs
     * @param context the context
     * @return the consume orderly status
     */
    ConsumeOrderlyStatus consumeMessage(final List<ExtMessage> msgs,
                                        final ConsumeOrderlyContext context);
}
