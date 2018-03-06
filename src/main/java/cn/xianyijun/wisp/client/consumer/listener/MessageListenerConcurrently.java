package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.common.message.ExtMessage;

import java.util.List;

public interface MessageListenerConcurrently extends MessageListener {
    /**
     * It is not recommend to throw exception,rather than returning ConsumeConcurrentlyStatus.RECONSUME_LATER if
     * consumption failure
     *
     * @param msgs msgs.size() >= 1<br> DefaultMQPushConsumer.consumeMessageBatchMaxSize=1,you can modify here
     * @return The consume status
     */
    ConsumeConcurrentlyStatus consumeMessage(final List<ExtMessage> msgs,
                                             final ConsumeConcurrentlyContext context);
}

