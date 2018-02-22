package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.ExtBatchMessage;
import cn.xianyijun.wisp.common.sysflag.MessageSysFlag;
import cn.xianyijun.wisp.store.config.MessageStoreConfig;
import cn.xianyijun.wisp.store.config.StorePathConfigHelper;
import cn.xianyijun.wisp.store.index.IndexService;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Getter
@Slf4j
public class DefaultMessageStore implements MessageStore {

    private final MessageStoreConfig messageStoreConfig;
    private final StoreStatsService storeStatsService;
    private final BrokerStatsManager brokerStatsManager;
    private final MessageArrivingListener messageArrivingListener;
    private final BrokerConfig brokerConfig;

    private final LinkedList<CommitLogDispatcher> dispatcherList;

    private final ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;

    private final IndexService indexService;

    private final RunningFlags runningFlags = new RunningFlags();

    private StoreCheckpoint storeCheckpoint;

    private final TransientStorePool transientStorePool;

    public DefaultMessageStore(final MessageStoreConfig messageStoreConfig, final BrokerStatsManager brokerStatsManager,
                               final MessageArrivingListener messageArrivingListener, final BrokerConfig brokerConfig) throws IOException {

        this.storeStatsService = new StoreStatsService();
        this.messageArrivingListener = messageArrivingListener;
        this.brokerConfig = brokerConfig;
        this.messageStoreConfig = messageStoreConfig;
        this.brokerStatsManager = brokerStatsManager;
        this.indexService = new IndexService(this);

        this.consumeQueueTable = new ConcurrentHashMap<>(32);

        this.dispatcherList = new LinkedList<>();
        this.dispatcherList.addLast(new CommitLogDispatcherBuildConsumeQueue());
        this.dispatcherList.addLast(new CommitLogDispatcherBuildIndex());

        this.transientStorePool = new TransientStorePool(messageStoreConfig);
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
    public PutMessageResult putMessages(ExtBatchMessage messageExtBatch) {
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
    public ExtMessage lookMessageByOffset(long commitLogOffset) {
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
        return dispatcherList;
    }

    @Override
    public ConsumeQueue getConsumeQueue(String topic, int queueId) {
        return null;
    }

    public void putMessagePositionInfo(DispatchRequest dispatchRequest) {
        ConsumeQueue cq = this.findConsumeQueue(dispatchRequest.getTopic(), dispatchRequest.getQueueId());
        cq.putMessagePositionInfoWrapper(dispatchRequest);
    }

    public ConsumeQueue findConsumeQueue(String topic, int queueId) {
        ConcurrentMap<Integer, ConsumeQueue> map = consumeQueueTable.get(topic);
        if (null == map) {
            ConcurrentMap<Integer, ConsumeQueue> newMap = new ConcurrentHashMap<Integer, ConsumeQueue>(128);
            ConcurrentMap<Integer, ConsumeQueue> oldMap = consumeQueueTable.putIfAbsent(topic, newMap);
            if (oldMap != null) {
                map = oldMap;
            } else {
                map = newMap;
            }
        }

        ConsumeQueue logic = map.get(queueId);
        if (null == logic) {
            ConsumeQueue newLogic = new ConsumeQueue(
                    topic,
                    queueId,
                    StorePathConfigHelper.getStorePathConsumeQueue(this.messageStoreConfig.getStorePathRootDir()),
                    this.getMessageStoreConfig().getMapedFileSizeConsumeQueue(),
                    this);
            ConsumeQueue oldLogic = map.putIfAbsent(queueId, newLogic);
            if (oldLogic != null) {
                logic = oldLogic;
            } else {
                logic = newLogic;
            }
        }

        return logic;
    }


    class CommitLogDispatcherBuildConsumeQueue implements CommitLogDispatcher {

        @Override
        public void dispatch(DispatchRequest request) {
            final int tranType = MessageSysFlag.getTransactionValue(request.getSysFlag());
            switch (tranType) {
                case MessageSysFlag.TRANSACTION_NOT_TYPE:
                case MessageSysFlag.TRANSACTION_COMMIT_TYPE:
                    DefaultMessageStore.this.putMessagePositionInfo(request);
                    break;
                case MessageSysFlag.TRANSACTION_PREPARED_TYPE:
                case MessageSysFlag.TRANSACTION_ROLLBACK_TYPE:
                    break;
                default:
                    break;
            }
        }
    }

    class CommitLogDispatcherBuildIndex implements CommitLogDispatcher {

        @Override
        public void dispatch(DispatchRequest request) {
            if (DefaultMessageStore.this.messageStoreConfig.isMessageIndexEnable()) {
                DefaultMessageStore.this.indexService.buildIndex(request);
            }
        }
    }
}
