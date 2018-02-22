package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.message.MessageExt;
import cn.xianyijun.wisp.common.message.MessageExtBatch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * The interface Message store.
 * @author xianyijun
 */
public interface MessageStore {

    /**
     * Load boolean.
     *
     * @return the boolean
     */
    boolean load();

    /**
     * Start.
     *
     * @throws Exception the exception
     */
    void start() throws Exception;

    /**
     * Shutdown.
     */
    void shutdown();

    /**
     * Destroy.
     */
    void destroy();

    /**
     * Put message put message result.
     *
     * @param msg the msg
     * @return the put message result
     */
    PutMessageResult putMessage(final MessageExtBrokerInner msg);

    /**
     * Put messages put message result.
     *
     * @param messageExtBatch the message ext batch
     * @return the put message result
     */
    PutMessageResult putMessages(final MessageExtBatch messageExtBatch);

    /**
     * Gets message.
     *
     * @param group         the group
     * @param topic         the topic
     * @param queueId       the queue id
     * @param offset        the offset
     * @param maxMsgNums    the max msg nums
     * @param messageFilter the message filter
     * @return the message
     */
    GetMessageResult getMessage(final String group, final String topic, final int queueId,
                                final long offset, final int maxMsgNums, final MessageFilter messageFilter);

    /**
     * Gets max offset in queue.
     *
     * @param topic   the topic
     * @param queueId the queue id
     * @return the max offset in queue
     */
    long getMaxOffsetInQueue(final String topic, final int queueId);

    /**
     * Gets min offset in queue.
     *
     * @param topic   the topic
     * @param queueId the queue id
     * @return the min offset in queue
     */
    long getMinOffsetInQueue(final String topic, final int queueId);

    /**
     * Gets commit log offset in queue.
     *
     * @param topic              the topic
     * @param queueId            the queue id
     * @param consumeQueueOffset the consume queue offset
     * @return the commit log offset in queue
     */
    long getCommitLogOffsetInQueue(final String topic, final int queueId, final long consumeQueueOffset);

    /**
     * Gets offset in queue by time.
     *
     * @param topic     the topic
     * @param queueId   the queue id
     * @param timestamp the timestamp
     * @return the offset in queue by time
     */
    long getOffsetInQueueByTime(final String topic, final int queueId, final long timestamp);

    /**
     * Look message by offset message ext.
     *
     * @param commitLogOffset the commit log offset
     * @return the message ext
     */
    MessageExt lookMessageByOffset(final long commitLogOffset);

    /**
     * Select one message by offset select mapped buffer result.
     *
     * @param commitLogOffset the commit log offset
     * @return the select mapped buffer result
     */
    SelectMappedBufferResult selectOneMessageByOffset(final long commitLogOffset);

    /**
     * Select one message by offset select mapped buffer result.
     *
     * @param commitLogOffset the commit log offset
     * @param msgSize         the msg size
     * @return the select mapped buffer result
     */
    SelectMappedBufferResult selectOneMessageByOffset(final long commitLogOffset, final int msgSize);

    /**
     * Gets running data info.
     *
     * @return the running data info
     */
    String getRunningDataInfo();

    /**
     * Gets runtime info.
     *
     * @return the runtime info
     */
    HashMap<String, String> getRuntimeInfo();

    /**
     * Gets max phy offset.
     *
     * @return the max phy offset
     */
    long getMaxPhyOffset();

    /**
     * Gets min phy offset.
     *
     * @return the min phy offset
     */
    long getMinPhyOffset();

    /**
     * Gets earliest message time.
     *
     * @param topic   the topic
     * @param queueId the queue id
     * @return the earliest message time
     */
    long getEarliestMessageTime(final String topic, final int queueId);

    /**
     * Gets earliest message time.
     *
     * @return the earliest message time
     */
    long getEarliestMessageTime();

    /**
     * Gets message store time stamp.
     *
     * @param topic              the topic
     * @param queueId            the queue id
     * @param consumeQueueOffset the consume queue offset
     * @return the message store time stamp
     */
    long getMessageStoreTimeStamp(final String topic, final int queueId, final long consumeQueueOffset);

    /**
     * Gets message total in queue.
     *
     * @param topic   the topic
     * @param queueId the queue id
     * @return the message total in queue
     */
    long getMessageTotalInQueue(final String topic, final int queueId);

    /**
     * Gets commit log data.
     *
     * @param offset the offset
     * @return the commit log data
     */
    SelectMappedBufferResult getCommitLogData(final long offset);

    /**
     * Append to commit log boolean.
     *
     * @param startOffset the start offset
     * @param data        the data
     * @return the boolean
     */
    boolean appendToCommitLog(final long startOffset, final byte[] data);

    /**
     * Execute delete files manually.
     */
    void executeDeleteFilesManually();

    /**
     * Query message query message result.
     *
     * @param topic  the topic
     * @param key    the key
     * @param maxNum the max num
     * @param begin  the begin
     * @param end    the end
     * @return the query message result
     */
    QueryMessageResult queryMessage(final String topic, final String key, final int maxNum, final long begin,
                                    final long end);

    /**
     * Update ha master address.
     *
     * @param newAddr the new addr
     */
    void updateHaMasterAddress(final String newAddr);

    /**
     * Slave fall behind much long.
     *
     * @return the long
     */
    long slaveFallBehindMuch();

    /**
     * Now long.
     *
     * @return the long
     */
    long now();

    /**
     * Clean unused topic int.
     *
     * @param topics the topics
     * @return the int
     */
    int cleanUnusedTopic(final Set<String> topics);

    /**
     * Clean expired consumer queue.
     */
    void cleanExpiredConsumerQueue();

    /**
     * Check in disk by consume offset boolean.
     *
     * @param topic         the topic
     * @param queueId       the queue id
     * @param consumeOffset the consume offset
     * @return the boolean
     */
    boolean checkInDiskByConsumeOffset(final String topic, final int queueId, long consumeOffset);

    /**
     * Dispatch behind bytes long.
     *
     * @return the long
     */
    long dispatchBehindBytes();

    /**
     * Flush long.
     *
     * @return the long
     */
    long flush();

    /**
     * Reset write offset boolean.
     *
     * @param phyOffset the phy offset
     * @return the boolean
     */
    boolean resetWriteOffset(long phyOffset);

    /**
     * Gets confirm offset.
     *
     * @return the confirm offset
     */
    long getConfirmOffset();

    /**
     * Sets confirm offset.
     *
     * @param phyOffset the phy offset
     */
    void setConfirmOffset(long phyOffset);

    /**
     * Is os page cache busy boolean.
     *
     * @return the boolean
     */
    boolean isOSPageCacheBusy();

    /**
     * Lock time mills long.
     *
     * @return the long
     */
    long lockTimeMills();

    /**
     * Is transient store pool deficient boolean.
     *
     * @return the boolean
     */
    boolean isTransientStorePoolDeficient();

    /**
     * Gets dispatcher list.
     *
     * @return the dispatcher list
     */
    LinkedList<CommitLogDispatcher> getDispatcherList();

    /**
     * Gets consume queue.
     *
     * @param topic   the topic
     * @param queueId the queue id
     * @return the consume queue
     */
    ConsumeQueue getConsumeQueue(String topic, int queueId);
}
