package cn.xianyijun.wisp.client.consumer.store;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;

import java.util.Map;
import java.util.Set;

/**
 * The interface Offset store.
 *
 * @author xianyijun
 */
public interface OffsetStore {
    /**
     * Load.
     *
     * @throws ClientException the client exception
     */
    void load() throws ClientException;

    /**
     * Update offset.
     *
     * @param mq           the mq
     * @param offset       the offset
     * @param increaseOnly the increase only
     */
    void updateOffset(final MessageQueue mq, final long offset, final boolean increaseOnly);

    /**
     * Read offset long.
     *
     * @param mq   the mq
     * @param type the type
     * @return the long
     */
    long readOffset(final MessageQueue mq, final ReadOffsetType type);

    /**
     * Persist all.
     *
     * @param mqs the mqs
     */
    void persistAll(final Set<MessageQueue> mqs);

    /**
     * Persist.
     *
     * @param mq the mq
     */
    void persist(final MessageQueue mq);

    /**
     * Remove offset.
     *
     * @param mq the mq
     */
    void removeOffset(MessageQueue mq);

    /**
     * Clone offset table map.
     *
     * @param topic the topic
     * @return the map
     */
    Map<MessageQueue, Long> cloneOffsetTable(String topic);

    /**
     * Update consume offset to broker.
     *
     * @param mq       the mq
     * @param offset   the offset
     * @param isOneway the is oneway
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException,
            BrokerException, InterruptedException, ClientException;
}
