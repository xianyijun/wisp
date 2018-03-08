package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.consumer.listener.MessageListenerConcurrently;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerOrderly;
import cn.xianyijun.wisp.exception.ClientException;

/**
 * The interface Push consumer.
 */
public interface PushConsumer extends Consumer {

    /**
     * Subscribe.
     *
     * @param topic         the topic
     * @param subExpression the sub expression
     * @throws ClientException the client exception
     */
    void subscribe(final String topic, final String subExpression) throws ClientException;

    /**
     * Register message listener.
     *
     * @param messageListener the message listener
     */
    void registerMessageListener(final MessageListenerConcurrently messageListener);

    /**
     * Register message listener.
     *
     * @param messageListener the message listener
     */
    void registerMessageListener(final MessageListenerOrderly messageListener);

    /**
     * Start.
     *
     * @throws ClientException the client exception
     */
    void start() throws ClientException;
}
