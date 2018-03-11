package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;

/**
 * The interface Pull consumer.
 *
 * @author xianyijun
 */
public interface PullConsumer extends Consumer {

    /**
     * Start.
     *
     * @throws ClientException the client exception
     */
    void start() throws ClientException;

    /**
     * Shutdown.
     */
    void shutdown();

    /**
     * Register message queue listener.
     *
     * @param topic    the topic
     * @param listener the listener
     */
    void registerMessageQueueListener(final String topic, final MessageQueueListener listener);

    /**
     * Pull pull result.
     *
     * @param mq            the mq
     * @param subExpression the sub expression
     * @param offset        the offset
     * @param maxNums       the max nums
     * @return the pull result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    PullResult pull(final MessageQueue mq, final String subExpression, final long offset,
                    final int maxNums) throws ClientException, RemotingException, BrokerException,
            InterruptedException;
}
