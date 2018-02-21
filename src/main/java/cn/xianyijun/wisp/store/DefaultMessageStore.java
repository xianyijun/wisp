package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.common.message.MessageExt;
import cn.xianyijun.wisp.common.message.MessageExtBatch;
import cn.xianyijun.wisp.store.config.MessageStoreConfig;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author xianyijun
 */
@Getter
public class DefaultMessageStore implements MessageStore {

    private final MessageStoreConfig messageStoreConfig;
    private final StoreStatsService storeStatsService;
    private final BrokerStatsManager brokerStatsManager;
    private final MessageArrivingListener messageArrivingListener;
    private final BrokerConfig brokerConfig;

    public DefaultMessageStore(final MessageStoreConfig messageStoreConfig, final BrokerStatsManager brokerStatsManager,
                               final MessageArrivingListener messageArrivingListener, final BrokerConfig brokerConfig) throws IOException {

        this.storeStatsService = new StoreStatsService();
        this.messageArrivingListener = messageArrivingListener;
        this.brokerConfig = brokerConfig;
        this.messageStoreConfig = messageStoreConfig;
        this.brokerStatsManager = brokerStatsManager;
    }

    @Override
    public boolean load() {
        return false;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public PutMessageResult putMessage(MessageExtBrokerInner msg) {
        return null;
    }

    @Override
    public PutMessageResult putMessages(MessageExtBatch messageExtBatch) {
        return null;
    }

    @Override
    public GetMessageResult getMessage(String group, String topic, int queueId, long offset, int maxMsgNums, MessageFilter messageFilter) {
        return null;
    }

    @Override
    public long getMaxOffsetInQueue(String topic, int queueId) {
        return 0;
    }

    @Override
    public long getMinOffsetInQueue(String topic, int queueId) {
        return 0;
    }

    @Override
    public long getCommitLogOffsetInQueue(String topic, int queueId, long consumeQueueOffset) {
        return 0;
    }

    @Override
    public long getOffsetInQueueByTime(String topic, int queueId, long timestamp) {
        return 0;
    }

    @Override
    public MessageExt lookMessageByOffset(long commitLogOffset) {
        return null;
    }

    @Override
    public SelectMappedBufferResult selectOneMessageByOffset(long commitLogOffset) {
        return null;
    }

    @Override
    public SelectMappedBufferResult selectOneMessageByOffset(long commitLogOffset, int msgSize) {
        return null;
    }

    @Override
    public String getRunningDataInfo() {
        return null;
    }

    @Override
    public HashMap<String, String> getRuntimeInfo() {
        return null;
    }

    @Override
    public long getMaxPhyOffset() {
        return 0;
    }

    @Override
    public long getMinPhyOffset() {
        return 0;
    }

    @Override
    public long getEarliestMessageTime(String topic, int queueId) {
        return 0;
    }

    @Override
    public long getEarliestMessageTime() {
        return 0;
    }

    @Override
    public long getMessageStoreTimeStamp(String topic, int queueId, long consumeQueueOffset) {
        return 0;
    }

    @Override
    public long getMessageTotalInQueue(String topic, int queueId) {
        return 0;
    }

    @Override
    public SelectMappedBufferResult getCommitLogData(long offset) {
        return null;
    }

    @Override
    public boolean appendToCommitLog(long startOffset, byte[] data) {
        return false;
    }

    @Override
    public void executeDeleteFilesManually() {

    }

    @Override
    public QueryMessageResult queryMessage(String topic, String key, int maxNum, long begin, long end) {
        return null;
    }

    @Override
    public void updateHaMasterAddress(String newAddr) {

    }

    @Override
    public long slaveFallBehindMuch() {
        return 0;
    }

    @Override
    public long now() {
        return 0;
    }

    @Override
    public int cleanUnusedTopic(Set<String> topics) {
        return 0;
    }

    @Override
    public void cleanExpiredConsumerQueue() {

    }

    @Override
    public boolean checkInDiskByConsumeOffset(String topic, int queueId, long consumeOffset) {
        return false;
    }

    @Override
    public long dispatchBehindBytes() {
        return 0;
    }

    @Override
    public long flush() {
        return 0;
    }

    @Override
    public boolean resetWriteOffset(long phyOffset) {
        return false;
    }

    @Override
    public long getConfirmOffset() {
        return 0;
    }

    @Override
    public void setConfirmOffset(long phyOffset) {

    }

    @Override
    public boolean isOSPageCacheBusy() {
        return false;
    }

    @Override
    public long lockTimeMills() {
        return 0;
    }

    @Override
    public boolean isTransientStorePoolDeficient() {
        return false;
    }

    @Override
    public LinkedList<CommitLogDispatcher> getDispatcherList() {
        return null;
    }

    @Override
    public ConsumeQueue getConsumeQueue(String topic, int queueId) {
        return null;
    }
}
