package cn.xianyijun.wisp.broker.plugin;

import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.store.CommitLogDispatcher;
import cn.xianyijun.wisp.store.ConsumeQueue;
import cn.xianyijun.wisp.store.MessageStore;
import cn.xianyijun.wisp.store.QueryMessageResult;
import cn.xianyijun.wisp.store.SelectMappedBufferResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author xianyijun
 */
@AllArgsConstructor
@Slf4j
@Getter
public abstract class AbstractPluginMessageStore implements MessageStore {
    protected MessageStore next = null;
    protected MessageStorePluginContext context;

    @Override
    public void start() throws Exception {
        next.start();
    }

    @Override
    public void shutdown() {
        next.shutdown();
    }

    @Override
    public void destroy() {
        next.destroy();
    }

    @Override
    public long getMaxOffsetInQueue(String topic, int queueId) {
        return next.getMaxOffsetInQueue(topic, queueId);
    }

    @Override
    public long getMinOffsetInQueue(String topic, int queueId) {
        return next.getMinOffsetInQueue(topic, queueId);
    }

    @Override
    public long getCommitLogOffsetInQueue(String topic, int queueId, long consumeQueueOffset) {
        return next.getCommitLogOffsetInQueue(topic, queueId, consumeQueueOffset);
    }

    @Override
    public long getOffsetInQueueByTime(String topic, int queueId, long timestamp) {
        return next.getOffsetInQueueByTime(topic, queueId, timestamp);
    }

    @Override
    public ExtMessage lookMessageByOffset(long commitLogOffset) {
        return next.lookMessageByOffset(commitLogOffset);
    }

    @Override
    public SelectMappedBufferResult selectOneMessageByOffset(long commitLogOffset) {
        return next.selectOneMessageByOffset(commitLogOffset);
    }

    @Override
    public SelectMappedBufferResult selectOneMessageByOffset(long commitLogOffset, int msgSize) {
        return next.selectOneMessageByOffset(commitLogOffset, msgSize);
    }

    @Override
    public String getRunningDataInfo() {
        return next.getRunningDataInfo();
    }

    @Override
    public HashMap<String, String> getRuntimeInfo() {
        return next.getRuntimeInfo();
    }

    @Override
    public long getMaxPhyOffset() {
        return next.getMaxPhyOffset();
    }

    @Override
    public long getMinPhyOffset() {
        return next.getMinPhyOffset();
    }

    @Override
    public long getEarliestMessageTime(String topic, int queueId) {
        return next.getEarliestMessageTime(topic, queueId);
    }

    @Override
    public long getMessageStoreTimeStamp(String topic, int queueId, long consumeQueueOffset) {
        return next.getMessageStoreTimeStamp(topic, queueId, consumeQueueOffset);
    }

    @Override
    public long getMessageTotalInQueue(String topic, int queueId) {
        return next.getMessageTotalInQueue(topic, queueId);
    }

    @Override
    public SelectMappedBufferResult getCommitLogData(long offset) {
        return next.getCommitLogData(offset);
    }

    @Override
    public boolean appendToCommitLog(long startOffset, byte[] data) {
        return next.appendToCommitLog(startOffset, data);
    }

    @Override
    public void executeDeleteFilesManually() {
        next.executeDeleteFilesManually();
    }

    @Override
    public QueryMessageResult queryMessage(String topic, String key, int maxNum, long begin,
                                           long end) {
        return next.queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    public void updateHaMasterAddress(String newAddr) {
        next.updateHaMasterAddress(newAddr);
    }

    @Override
    public long slaveFallBehindMuch() {
        return next.slaveFallBehindMuch();
    }

    @Override
    public long now() {
        return next.now();
    }

    @Override
    public int cleanUnusedTopic(Set<String> topics) {
        return next.cleanUnusedTopic(topics);
    }

    @Override
    public void cleanExpiredConsumerQueue() {
        next.cleanExpiredConsumerQueue();
    }

    @Override
    public boolean checkInDiskByConsumeOffset(String topic, int queueId, long consumeOffset) {
        return next.checkInDiskByConsumeOffset(topic, queueId, consumeOffset);
    }

    @Override
    public long dispatchBehindBytes() {
        return next.dispatchBehindBytes();
    }

    @Override
    public long flush() {
        return next.flush();
    }

    @Override
    public boolean resetWriteOffset(long phyOffset) {
        return next.resetWriteOffset(phyOffset);
    }

    @Override
    public long getConfirmOffset() {
        return next.getConfirmOffset();
    }

    @Override
    public void setConfirmOffset(long phyOffset) {
        next.setConfirmOffset(phyOffset);
    }

    @Override
    public LinkedList<CommitLogDispatcher> getDispatcherList() {
        return next.getDispatcherList();
    }

    @Override
    public ConsumeQueue getConsumeQueue(String topic, int queueId) {
        return next.getConsumeQueue(topic, queueId);
    }
}