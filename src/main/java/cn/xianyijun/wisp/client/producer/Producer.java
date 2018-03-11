package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.client.MQAdmin;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;

import java.util.Collection;
import java.util.List;

/**
 * The interface Mq producer.
 * @author xianyijun
 */
public interface Producer extends MQAdmin{

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
     * Fetch publish message queues list.
     *
     * @param topic the topic
     * @return the list
     * @throws ClientException the client exception
     */
    List<MessageQueue> fetchPublishMessageQueues(final String topic) throws ClientException;

    /**
     * Send send result.
     *
     * @param msg the msg
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Message msg) throws ClientException, RemotingException, BrokerException,
            InterruptedException;

    /**
     * Send send result.
     *
     * @param msg     the msg
     * @param timeout the timeout
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Message msg, final long timeout) throws ClientException,
            RemotingException, BrokerException, InterruptedException;

    /**
     * Send.
     *
     * @param msg          the msg
     * @param sendCallback the send callback
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void send(final Message msg, final SendCallback sendCallback) throws ClientException,
            RemotingException, InterruptedException;

    /**
     * Send.
     *
     * @param msg          the msg
     * @param sendCallback the send callback
     * @param timeout      the timeout
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void send(final Message msg, final SendCallback sendCallback, final long timeout)
            throws ClientException, RemotingException, InterruptedException;

    /**
     * Send oneway.
     *
     * @param msg the msg
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void sendOneWay(final Message msg) throws ClientException, RemotingException,
            InterruptedException;

    /**
     * Send send result.
     *
     * @param msg the msg
     * @param mq  the mq
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Message msg, final MessageQueue mq) throws ClientException,
            RemotingException, BrokerException, InterruptedException;

    /**
     * Send send result.
     *
     * @param msg     the msg
     * @param mq      the mq
     * @param timeout the timeout
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Message msg, final MessageQueue mq, final long timeout)
            throws ClientException, RemotingException, BrokerException, InterruptedException;

    /**
     * Send.
     *
     * @param msg          the msg
     * @param mq           the mq
     * @param sendCallback the send callback
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void send(final Message msg, final MessageQueue mq, final SendCallback sendCallback)
            throws ClientException, RemotingException, InterruptedException;

    /**
     * Send.
     *
     * @param msg          the msg
     * @param mq           the mq
     * @param sendCallback the send callback
     * @param timeout      the timeout
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void send(final Message msg, final MessageQueue mq, final SendCallback sendCallback, long timeout)
            throws ClientException, RemotingException, InterruptedException;

    /**
     * Send oneway.
     *
     * @param msg the msg
     * @param mq  the mq
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void sendOneWay(final Message msg, final MessageQueue mq) throws ClientException,
            RemotingException, InterruptedException;

    /**
     * Send send result.
     *
     * @param msg      the msg
     * @param selector the selector
     * @param arg      the arg
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Message msg, final MessageQueueSelector selector, final Object arg)
            throws ClientException, RemotingException, BrokerException, InterruptedException;

    /**
     * Send send result.
     *
     * @param msg      the msg
     * @param selector the selector
     * @param arg      the arg
     * @param timeout  the timeout
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Message msg, final MessageQueueSelector selector, final Object arg,
                    final long timeout) throws ClientException, RemotingException, BrokerException,
            InterruptedException;

    /**
     * Send.
     *
     * @param msg          the msg
     * @param selector     the selector
     * @param arg          the arg
     * @param sendCallback the send callback
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void send(final Message msg, final MessageQueueSelector selector, final Object arg,
              final SendCallback sendCallback) throws ClientException, RemotingException,
            InterruptedException;

    /**
     * Send.
     *
     * @param msg          the msg
     * @param selector     the selector
     * @param arg          the arg
     * @param sendCallback the send callback
     * @param timeout      the timeout
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void send(final Message msg, final MessageQueueSelector selector, final Object arg,
              final SendCallback sendCallback, final long timeout) throws ClientException, RemotingException,
            InterruptedException;

    /**
     * Send oneway.
     *
     * @param msg      the msg
     * @param selector the selector
     * @param arg      the arg
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     */
    void sendOneWay(final Message msg, final MessageQueueSelector selector, final Object arg)
            throws ClientException, RemotingException, InterruptedException;

    /**
     * Send message in transaction transaction send result.
     *
     * @param msg          the msg
     * @param tranExecuter the tran executer
     * @param arg          the arg
     * @return the transaction send result
     * @throws ClientException the client exception
     */
    TransactionSendResult sendMessageInTransaction(final Message msg,
                                                   final LocalTransactionExecutor tranExecuter, final Object arg) throws ClientException;

    /**
     * Send send result.
     *
     * @param msgs the msgs
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Collection<Message> msgs) throws ClientException, RemotingException, BrokerException,
            InterruptedException;

    /**
     * Send send result.
     *
     * @param msgs    the msgs
     * @param timeout the timeout
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Collection<Message> msgs, final long timeout) throws ClientException,
            RemotingException, BrokerException, InterruptedException;

    /**
     * Send send result.
     *
     * @param msgs the msgs
     * @param mq   the mq
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Collection<Message> msgs, final MessageQueue mq) throws ClientException,
            RemotingException, BrokerException, InterruptedException;

    /**
     * Send send result.
     *
     * @param msgs    the msgs
     * @param mq      the mq
     * @param timeout the timeout
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    SendResult send(final Collection<Message> msgs, final MessageQueue mq, final long timeout)
            throws ClientException, RemotingException, BrokerException, InterruptedException;

}
