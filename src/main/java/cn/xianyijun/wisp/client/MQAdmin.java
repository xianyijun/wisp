package cn.xianyijun.wisp.client;

import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;

/**
 * The interface Mq admin.
 * @author xianyijun
 */
public interface MQAdmin {
    /**
     * Create topic.
     *
     * @param key      the key
     * @param newTopic the new topic
     * @param queueNum the queue num
     * @throws ClientException the client exception
     */
    void createTopic(final String key, final String newTopic, final int queueNum)
            throws ClientException;

    /**
     * Create topic.
     *
     * @param key          the key
     * @param newTopic     the new topic
     * @param queueNum     the queue num
     * @param topicSysFlag the topic sys flag
     * @throws ClientException the client exception
     */
    void createTopic(String key, String newTopic, int queueNum, int topicSysFlag)
            throws ClientException;

    /**
     * Search offset long.
     *
     * @param mq        the mq
     * @param timestamp the timestamp
     * @return the long
     * @throws ClientException the client exception
     */
    long searchOffset(final MessageQueue mq, final long timestamp) throws ClientException;

    /**
     * Max offset long.
     *
     * @param mq the mq
     * @return the long
     * @throws ClientException the client exception
     */
    long maxOffset(final MessageQueue mq) throws ClientException;

    /**
     * Min offset long.
     *
     * @param mq the mq
     * @return the long
     * @throws ClientException the client exception
     */
    long minOffset(final MessageQueue mq) throws ClientException;

    /**
     * Earliest msg store time long.
     *
     * @param mq the mq
     * @return the long
     * @throws ClientException the client exception
     */
    long earliestMsgStoreTime(final MessageQueue mq) throws ClientException;

    /**
     * View message ext message.
     *
     * @param offsetMsgId the offset msg id
     * @return the ext message
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    ExtMessage viewMessage(final String offsetMsgId) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Query message query result.
     *
     * @param topic  the topic
     * @param key    the key
     * @param maxNum the max num
     * @param begin  the begin
     * @param end    the end
     * @return the query result
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    QueryResult queryMessage(final String topic, final String key, final int maxNum, final long begin,
                             final long end) throws ClientException, InterruptedException;

    /**
     * View message ext message.
     *
     * @param topic the topic
     * @param msgId the msg id
     * @return the ext message
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    ExtMessage viewMessage(String topic,
                           String msgId) throws RemotingException, BrokerException, InterruptedException, ClientException;

}