package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.consumer.listener.ConsumeMessageConcurrentlyService;
import cn.xianyijun.wisp.client.consumer.listener.ConsumeMessageOrderlyService;
import cn.xianyijun.wisp.client.consumer.listener.MessageListener;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerConcurrently;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerOrderly;
import cn.xianyijun.wisp.client.consumer.store.LocalFileOffsetStore;
import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.client.consumer.store.ReadOffsetType;
import cn.xianyijun.wisp.client.consumer.store.RemoteBrokerOffsetStore;
import cn.xianyijun.wisp.client.hook.ConsumeMessageContext;
import cn.xianyijun.wisp.client.hook.ConsumeMessageHook;
import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.client.stat.ConsumerStatsManager;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.filter.Filter;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumeStatus;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.body.ProcessQueueInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.common.sysflag.PullSysFlag;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class ConsumerPushDelegate implements ConsumerInner {

    private static final long PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION = 3000;

    private static final long PULL_TIME_DELAY_MILLS_WHEN_SUSPEND = 1000;

    private static final long PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL = 50;


    private static final long BROKER_SUSPEND_MAX_TIME_MILLIS = 1000 * 15;

    private static final long CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND = 1000 * 30;

    private final DefaultPushConsumer defaultPushConsumer;

    private final RPCHook rpcHook;

    private ConsumeMessageService consumeMessageService;

    private final ArrayList<ConsumeMessageHook> consumeMessageHookList = new ArrayList<ConsumeMessageHook>();

    private AbstractReBalance reBalance = new PushReBalance(this);

    private OffsetStore offsetStore;

    private volatile boolean pause = false;

    private boolean consumeOrderly = false;

    private volatile ServiceState serviceState = ServiceState.CREATE_JUST;

    private ClientInstance clientFactory;

    private PullConsumerWrapper pullConsumerWrapper;

    private final ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<>();

    private final long consumerStartTimestamp = System.currentTimeMillis();

    @Setter
    private MessageListener messageListenerInner;

    private long queueFlowControlTimes = 0;

    private long queueMaxSpanFlowControlTimes = 0;

    public void sendMessageBack(ExtMessage msg, int delayLevel, final String brokerName)
            throws RemotingException, BrokerException, InterruptedException, ClientException {
        try {
            String brokerAddr = (null != brokerName) ? this.clientFactory.findBrokerAddressInPublish(brokerName)
                    : RemotingHelper.parseSocketAddressAddr(msg.getStoreHost());
            this.clientFactory.getClient().consumerSendMessageBack(brokerAddr, msg,
                    this.defaultPushConsumer.getConsumerGroup(), delayLevel, 5000, getMaxReConsumeTimes());
        } catch (Exception e) {
            log.error("sendMessageBack Exception, " + this.defaultPushConsumer.getConsumerGroup(), e);

            Message newMsg = new Message(MixAll.getRetryTopic(this.defaultPushConsumer.getConsumerGroup()), msg.getBody());

            String originMsgId = MessageAccessor.getOriginMessageId(msg);
            MessageAccessor.setOriginMessageId(newMsg, StringUtils.isBlank(originMsgId) ? msg.getMsgId() : originMsgId);

            newMsg.setFlag(msg.getFlag());
            MessageAccessor.setProperties(newMsg, msg.getProperties());
            MessageAccessor.putProperty(newMsg, MessageConst.PROPERTY_RETRY_TOPIC, msg.getTopic());
            MessageAccessor.setReConsumeTime(newMsg, String.valueOf(msg.getReConsumeTimes() + 1));
            MessageAccessor.setMaxReConsumeTimes(newMsg, String.valueOf(getMaxReConsumeTimes()));
            newMsg.setDelayTimeLevel(3 + msg.getReConsumeTimes());

            this.clientFactory.getDefaultProducer().send(newMsg);
        }
    }

    private int getMaxReConsumeTimes() {
        // default reconsume times: 16
        if (this.defaultPushConsumer.getMaxReConsumeTimes() == -1) {
            return 16;
        } else {
            return this.defaultPushConsumer.getMaxReConsumeTimes();
        }
    }

    public void pullMessage(final PullRequest pullRequest) {
        final ProcessQueue processQueue = pullRequest.getProcessQueue();
        if (processQueue.isDropped()) {
            log.info("the pull request[{}] is dropped.", pullRequest.toString());
            return;
        }

        pullRequest.getProcessQueue().setLastPullTimestamp(System.currentTimeMillis());

        try {
            this.makeSureStateOK();
        } catch (ClientException e) {
            log.warn("pullMessage exception, consumer state not ok", e);
            this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
            return;
        }

        if (this.isPause()) {
            log.warn("consumer was paused, execute pull request later. instanceName={}, group={}", this.defaultPushConsumer.getInstanceName(), this.defaultPushConsumer.getConsumerGroup());
            this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_SUSPEND);
            return;
        }

        long cachedMessageCount = processQueue.getMsgCount().get();
        long cachedMessageSizeInMiB = processQueue.getMsgSize().get() / (1024 * 1024);

        if (cachedMessageCount > this.defaultPushConsumer.getPullThresholdForQueue()) {
            this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
            if ((queueFlowControlTimes++ % 1000) == 0) {
                log.warn(
                        "the cached message count exceeds the threshold {}, so do flow control, minOffset={}, maxOffset={}, count={}, size={} MiB, pullRequest={}, flowControlTimes={}",
                        this.defaultPushConsumer.getPullThresholdForQueue(), processQueue.getMsgTreeMap().firstKey(), processQueue.getMsgTreeMap().lastKey(), cachedMessageCount, cachedMessageSizeInMiB, pullRequest, queueFlowControlTimes);
            }
            return;
        }

        if (cachedMessageSizeInMiB > this.defaultPushConsumer.getPullThresholdSizeForQueue()) {
            this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
            if ((queueFlowControlTimes++ % 1000) == 0) {
                log.warn(
                        "the cached message size exceeds the threshold {} MiB, so do flow control, minOffset={}, maxOffset={}, count={}, size={} MiB, pullRequest={}, flowControlTimes={}",
                        this.defaultPushConsumer.getPullThresholdSizeForQueue(), processQueue.getMsgTreeMap().firstKey(), processQueue.getMsgTreeMap().lastKey(), cachedMessageCount, cachedMessageSizeInMiB, pullRequest, queueFlowControlTimes);
            }
            return;
        }

        if (!this.consumeOrderly) {
            if (processQueue.getMaxSpan() > this.defaultPushConsumer.getConsumeConcurrentlyMaxSpan()) {
                this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_FLOW_CONTROL);
                if ((queueMaxSpanFlowControlTimes++ % 1000) == 0) {
                    log.warn(
                            "the queue's messages, span too long, so do flow control, minOffset={}, maxOffset={}, maxSpan={}, pullRequest={}, flowControlTimes={}",
                            processQueue.getMsgTreeMap().firstKey(), processQueue.getMsgTreeMap().lastKey(), processQueue.getMaxSpan(),
                            pullRequest, queueMaxSpanFlowControlTimes);
                }
                return;
            }
        } else {
            if (processQueue.isLocked()) {
                if (!pullRequest.isLockedFirst()) {
                    final long offset = this.reBalance.computePullFromWhere(pullRequest.getMessageQueue());
                    boolean brokerBusy = offset < pullRequest.getNextOffset();
                    log.info("the first time to pull message, so fix offset from broker. pullRequest: {} NewOffset: {} brokerBusy: {}",
                            pullRequest, offset, brokerBusy);
                    if (brokerBusy) {
                        log.info("[NOTIFYME]the first time to pull message, but pull request offset larger than broker consume offset. pullRequest: {} NewOffset: {}",
                                pullRequest, offset);
                    }

                    pullRequest.setLockedFirst(true);
                    pullRequest.setNextOffset(offset);
                }
            } else {
                this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
                log.info("pull message later because not locked in broker, {}", pullRequest);
                return;
            }
        }

        final SubscriptionData subscriptionData = this.reBalance.getSubscriptionInner().get(pullRequest.getMessageQueue().getTopic());
        if (null == subscriptionData) {
            this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
            log.warn("find the consumer's subscription failed, {}", pullRequest);
            return;
        }

        final long beginTimestamp = System.currentTimeMillis();

        PullCallback pullCallback = new PullCallback() {
            @Override
            public void onSuccess(PullResult pullResult) {
                if (pullResult != null) {
                    pullResult = ConsumerPushDelegate.this.pullConsumerWrapper.processPullResult(pullRequest.getMessageQueue(), pullResult,
                            subscriptionData);

                    switch (pullResult.getPullStatus()) {
                        case FOUND:
                            long prevRequestOffset = pullRequest.getNextOffset();
                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());
                            long pullRT = System.currentTimeMillis() - beginTimestamp;
                            ConsumerPushDelegate.this.getConsumerStatsManager().incPullRT(pullRequest.getConsumerGroup(),
                                    pullRequest.getMessageQueue().getTopic(), pullRT);

                            long firstMsgOffset = Long.MAX_VALUE;
                            if (pullResult.getMsgFoundList() == null || pullResult.getMsgFoundList().isEmpty()) {
                                ConsumerPushDelegate.this.executePullRequestImmediately(pullRequest);
                            } else {
                                firstMsgOffset = pullResult.getMsgFoundList().get(0).getQueueOffset();

                                ConsumerPushDelegate.this.getConsumerStatsManager().incPullTPS(pullRequest.getConsumerGroup(),
                                        pullRequest.getMessageQueue().getTopic(), pullResult.getMsgFoundList().size());

                                boolean dispathToConsume = processQueue.putMessage(pullResult.getMsgFoundList());
                                ConsumerPushDelegate.this.consumeMessageService.submitConsumeRequest(
                                        pullResult.getMsgFoundList(),
                                        processQueue,
                                        pullRequest.getMessageQueue(),
                                        dispathToConsume);

                                if (ConsumerPushDelegate.this.defaultPushConsumer.getPullInterval() > 0) {
                                    ConsumerPushDelegate.this.executePullRequestLater(pullRequest,
                                            ConsumerPushDelegate.this.defaultPushConsumer.getPullInterval());
                                } else {
                                    ConsumerPushDelegate.this.executePullRequestImmediately(pullRequest);
                                }
                            }

                            if (pullResult.getNextBeginOffset() < prevRequestOffset
                                    || firstMsgOffset < prevRequestOffset) {
                                log.warn(
                                        "[BUG] pull message result maybe data wrong, nextBeginOffset: {} firstMsgOffset: {} prevRequestOffset: {}",
                                        pullResult.getNextBeginOffset(),
                                        firstMsgOffset,
                                        prevRequestOffset);
                            }

                            break;
                        case NO_NEW_MSG:
                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());

                            ConsumerPushDelegate.this.correctTagsOffset(pullRequest);

                            ConsumerPushDelegate.this.executePullRequestImmediately(pullRequest);
                            break;
                        case NO_MATCHED_MSG:
                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());

                            ConsumerPushDelegate.this.correctTagsOffset(pullRequest);

                            ConsumerPushDelegate.this.executePullRequestImmediately(pullRequest);
                            break;
                        case OFFSET_ILLEGAL:
                            log.warn("the pull request offset illegal, {} {}",
                                    pullRequest.toString(), pullResult.toString());
                            pullRequest.setNextOffset(pullResult.getNextBeginOffset());

                            pullRequest.getProcessQueue().setDropped(true);
                            ConsumerPushDelegate.this.executeTaskLater(() -> {
                                try {
                                    ConsumerPushDelegate.this.offsetStore.updateOffset(pullRequest.getMessageQueue(),
                                            pullRequest.getNextOffset(), false);

                                    ConsumerPushDelegate.this.offsetStore.persist(pullRequest.getMessageQueue());

                                    ConsumerPushDelegate.this.reBalance.removeProcessQueue(pullRequest.getMessageQueue());

                                    log.warn("fix the pull request offset, {}", pullRequest);
                                } catch (Throwable e) {
                                    log.error("executeTaskLater Exception", e);
                                }
                            }, 10000);
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onException(Throwable e) {
                if (!pullRequest.getMessageQueue().getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    log.warn("execute the pull request exception", e);
                }

                ConsumerPushDelegate.this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
            }
        };

        boolean commitOffsetEnable = false;
        long commitOffsetValue = 0L;
        if (MessageModel.CLUSTERING == this.defaultPushConsumer.getMessageModel()) {
            commitOffsetValue = this.offsetStore.readOffset(pullRequest.getMessageQueue(), ReadOffsetType.READ_FROM_MEMORY);
            if (commitOffsetValue > 0) {
                commitOffsetEnable = true;
            }
        }

        String subExpression = null;
        boolean classFilter = false;
        SubscriptionData sd = this.reBalance.getSubscriptionInner().get(pullRequest.getMessageQueue().getTopic());
        if (sd != null) {
            if (this.defaultPushConsumer.isPostSubscriptionWhenPull() && !sd.isClassFilterMode()) {
                subExpression = sd.getSubString();
            }

            classFilter = sd.isClassFilterMode();
        }

        int sysFlag = PullSysFlag.buildSysFlag(
                commitOffsetEnable,
                true,
                subExpression != null,
                classFilter
        );
        try {
            this.pullConsumerWrapper.doPullKernel(
                    pullRequest.getMessageQueue(),
                    subExpression,
                    subscriptionData.getExpressionType(),
                    subscriptionData.getSubVersion(),
                    pullRequest.getNextOffset(),
                    this.defaultPushConsumer.getPullBatchSize(),
                    sysFlag,
                    commitOffsetValue,
                    BROKER_SUSPEND_MAX_TIME_MILLIS,
                    CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND,
                    CommunicationMode.ASYNC,
                    pullCallback
            );
        } catch (Exception e) {
            log.error("pullKernelImpl exception", e);
            this.executePullRequestLater(pullRequest, PULL_TIME_DELAY_MILLS_WHEN_EXCEPTION);
        }
    }


    private void executePullRequestLater(final PullRequest pullRequest, final long timeDelay) {
        this.clientFactory.getPullMessageService().executePullRequestLater(pullRequest, timeDelay);
    }

    public void executePullRequestImmediately(final PullRequest pullRequest) {
        this.clientFactory.getPullMessageService().executePullRequestImmediately(pullRequest);
    }

    private void correctTagsOffset(final PullRequest pullRequest) {
        if (0L == pullRequest.getProcessQueue().getMsgCount().get()) {
            this.offsetStore.updateOffset(pullRequest.getMessageQueue(), pullRequest.getNextOffset(), true);
        }
    }

    public void executeTaskLater(final Runnable r, final long timeDelay) {
        this.clientFactory.getPullMessageService().executeTaskLater(r, timeDelay);
    }

    public void adjustThreadPool() {
        long computeAccTotal = this.computeAccumulationTotal();
        long adjustThreadPoolNumsThreshold = this.defaultPushConsumer.getAdjustThreadPoolNumsThreshold();

        long incThreshold = (long) (adjustThreadPoolNumsThreshold * 1.0);

        long decThreshold = (long) (adjustThreadPoolNumsThreshold * 0.8);

        if (computeAccTotal >= incThreshold) {
            this.consumeMessageService.incCorePoolSize();
        }

        if (computeAccTotal < decThreshold) {
            this.consumeMessageService.decCorePoolSize();
        }
    }

    @Override
    public String groupName() {
        return this.defaultPushConsumer.getConsumerGroup();
    }

    @Override
    public MessageModel messageModel() {
        return this.defaultPushConsumer.getMessageModel();
    }

    @Override
    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_PASSIVELY;
    }

    @Override
    public ConsumeWhereEnum consumeFromWhere() {
        return this.defaultPushConsumer.getConsumeFromWhere();
    }

    @Override
    public Set<SubscriptionData> subscription() {
        return new HashSet<>(this.reBalance.getSubscriptionInner().values());
    }

    @Override
    public void doReBalance() {
        if (!this.pause) {
            this.reBalance.doReBalance(this.isConsumeOrderly());
        }
    }

    @Override
    public void persistConsumerOffset() {
        try {
            this.makeSureStateOK();
            Set<MessageQueue> allocateMq = this.reBalance.getProcessQueueTable().keySet();
            Set<MessageQueue> mqs = new HashSet<>(allocateMq);

            this.offsetStore.persistAll(mqs);
        } catch (Exception e) {
            log.error("group: " + this.defaultPushConsumer.getConsumerGroup() + " persistConsumerOffset exception", e);
        }
    }

    private void makeSureStateOK() throws ClientException {
        if (this.serviceState != ServiceState.RUNNING) {
            throw new ClientException("The consumer service state not OK, " + this.serviceState,
                    null);
        }
    }


    @Override
    public void updateTopicSubscribeInfo(String topic, Set<MessageQueue> info) {
        Map<String, SubscriptionData> subTable = this.getSubscriptionInner();
        if (subTable != null) {
            if (subTable.containsKey(topic)) {
                this.reBalance.topicSubscribeInfoTable.put(topic, info);
            }
        }
    }

    @Override
    public boolean isSubscribeTopicNeedUpdate(String topic) {
        Map<String, SubscriptionData> subTable = this.getSubscriptionInner();
        if (subTable != null) {
            if (subTable.containsKey(topic)) {
                return !this.reBalance.topicSubscribeInfoTable.containsKey(topic);
            }
        }
        return false;
    }

    @Override
    public boolean isUnitMode() {
        return this.defaultPushConsumer.isUnitMode();
    }

    @Override
    public ConsumerRunningInfo consumerRunningInfo() {
        ConsumerRunningInfo info = new ConsumerRunningInfo();

        Properties prop = MixAll.object2Properties(this.defaultPushConsumer);

        prop.put(ConsumerRunningInfo.PROP_CONSUME_ORDERLY, String.valueOf(this.consumeOrderly));
        prop.put(ConsumerRunningInfo.PROP_THREAD_POOL_CORE_SIZE, String.valueOf(this.consumeMessageService.getCorePoolSize()));
        prop.put(ConsumerRunningInfo.PROP_CONSUMER_START_TIMESTAMP, String.valueOf(this.consumerStartTimestamp));

        info.setProperties(prop);

        Set<SubscriptionData> subSet = this.subscription();
        info.getSubscriptionSet().addAll(subSet);

        for (Map.Entry<MessageQueue, ProcessQueue> next : this.reBalance.getProcessQueueTable().entrySet()) {
            MessageQueue mq = next.getKey();
            ProcessQueue pq = next.getValue();

            ProcessQueueInfo pqinfo = new ProcessQueueInfo();
            pqinfo.setCommitOffset(this.offsetStore.readOffset(mq, ReadOffsetType.MEMORY_FIRST_THEN_STORE));
            pq.fillProcessQueueInfo(pqinfo);
            info.getMqTable().put(mq, pqinfo);
        }

        for (SubscriptionData sd : subSet) {
            ConsumeStatus consumeStatus = this.clientFactory.getConsumerStatsManager().consumeStatus(this.groupName(), sd.getTopic());
            info.getStatusTable().put(sd.getTopic(), consumeStatus);
        }

        return info;

    }

    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        return this.clientFactory.getAdmin().searchOffset(mq, timestamp);
    }


    private long computeAccumulationTotal() {
        long msgAccTotal = 0;
        ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = this.reBalance.getProcessQueueTable();
        for (Map.Entry<MessageQueue, ProcessQueue> next : processQueueTable.entrySet()) {
            ProcessQueue value = next.getValue();
            msgAccTotal += value.getMsgAccCnt();
        }

        return msgAccTotal;
    }

    public void suspend() {
        this.pause = true;
        log.info("suspend this consumer, {}", this.defaultPushConsumer.getConsumerGroup());
    }

    public void resume() {
        this.pause = false;
        doReBalance();
        log.info("resume this consumer, {}", this.defaultPushConsumer.getConsumerGroup());
    }

    public void updateConsumeOffset(MessageQueue mq, long offset) {
        this.offsetStore.updateOffset(mq, offset, false);
    }

    public synchronized void start() throws ClientException {
        switch (this.serviceState) {
            case CREATE_JUST:
                log.info("the consumer [{}] start beginning. messageModel={}, isUnitMode={}", this.defaultPushConsumer.getConsumerGroup(),
                        this.defaultPushConsumer.getMessageModel(), this.defaultPushConsumer.isUnitMode());
                this.serviceState = ServiceState.START_FAILED;

                this.checkConfig();

                this.copySubscription();

                if (this.defaultPushConsumer.getMessageModel() == MessageModel.CLUSTERING) {
                    this.defaultPushConsumer.changeInstanceNameToPID();
                }

                this.clientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultPushConsumer, this.rpcHook);

                this.getReBalance().setConsumerGroup(this.defaultPushConsumer.getConsumerGroup());
                this.getReBalance().setMessageModel(this.defaultPushConsumer.getMessageModel());
                this.getReBalance().setAllocateMessageQueueStrategy(this.defaultPushConsumer.getAllocateMessageQueueStrategy());
                this.getReBalance().setClientFactory(this.clientFactory);

                this.pullConsumerWrapper = new PullConsumerWrapper(
                        clientFactory,
                        this.defaultPushConsumer.getConsumerGroup(), isUnitMode());
                this.pullConsumerWrapper.setFilterMessageHookList(filterMessageHookList);

                if (this.defaultPushConsumer.getOffsetStore() != null) {
                    this.offsetStore = this.defaultPushConsumer.getOffsetStore();
                } else {
                    switch (this.defaultPushConsumer.getMessageModel()) {
                        case BROADCASTING:
                            this.offsetStore = new LocalFileOffsetStore(this.clientFactory, this.defaultPushConsumer.getConsumerGroup());
                            break;
                        case CLUSTERING:
                            this.offsetStore = new RemoteBrokerOffsetStore(this.clientFactory, this.defaultPushConsumer.getConsumerGroup());
                            break;
                        default:
                            break;
                    }
                    this.defaultPushConsumer.setOffsetStore(this.offsetStore);
                }
                this.offsetStore.load();

                if (this.getMessageListenerInner() instanceof MessageListenerOrderly) {
                    this.consumeOrderly = true;
                    this.consumeMessageService =
                            new ConsumeMessageOrderlyService(this, (MessageListenerOrderly) this.getMessageListenerInner());
                } else if (this.getMessageListenerInner() instanceof MessageListenerConcurrently) {
                    this.consumeOrderly = false;
                    this.consumeMessageService =
                            new ConsumeMessageConcurrentlyService(this, (MessageListenerConcurrently) this.getMessageListenerInner());
                }

                this.consumeMessageService.start();

                boolean registerOK = clientFactory.registerConsumer(this.defaultPushConsumer.getConsumerGroup(), this);
                if (!registerOK) {
                    this.serviceState = ServiceState.CREATE_JUST;
                    this.consumeMessageService.shutdown();
                    throw new ClientException("The consumer group[" + this.defaultPushConsumer.getConsumerGroup()
                            + "] has been created before, specify another name please.",
                            null);
                }

                clientFactory.start();
                log.info("the consumer [{}] start OK.", this.defaultPushConsumer.getConsumerGroup());
                this.serviceState = ServiceState.RUNNING;
                break;
            case RUNNING:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
                throw new ClientException("The PushConsumer service state not OK, maybe started once, "
                        + this.serviceState,
                        null);
            default:
                break;
        }

        this.updateTopicSubscribeInfoWhenSubscriptionChanged();
        this.clientFactory.checkClientInBroker();
        this.clientFactory.sendHeartbeatToAllBrokerWithLock();
        this.clientFactory.reBalanceImmediately();
    }

    private void copySubscription() throws ClientException {
        try {
            Map<String, String> sub = this.defaultPushConsumer.getSubscription();
            if (sub != null) {
                for (final Map.Entry<String, String> entry : sub.entrySet()) {
                    final String topic = entry.getKey();
                    final String subString = entry.getValue();
                    SubscriptionData subscriptionData = Filter.buildSubscriptionData(this.defaultPushConsumer.getConsumerGroup(),
                            topic, subString);
                    this.reBalance.getSubscriptionInner().put(topic, subscriptionData);
                }
            }

            if (null == this.messageListenerInner) {
                this.messageListenerInner = this.defaultPushConsumer.getMessageListener();
            }

            switch (this.defaultPushConsumer.getMessageModel()) {
                case BROADCASTING:
                    break;
                case CLUSTERING:
                    final String retryTopic = MixAll.getRetryTopic(this.defaultPushConsumer.getConsumerGroup());
                    SubscriptionData subscriptionData = Filter.buildSubscriptionData(this.defaultPushConsumer.getConsumerGroup(),
                            retryTopic, SubscriptionData.SUB_ALL);
                    this.reBalance.getSubscriptionInner().put(retryTopic, subscriptionData);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw new ClientException("subscription exception", e);
        }
    }


    private void updateTopicSubscribeInfoWhenSubscriptionChanged() {
        Map<String, SubscriptionData> subTable = this.getSubscriptionInner();
        if (subTable != null) {
            for (final Map.Entry<String, SubscriptionData> entry : subTable.entrySet()) {
                final String topic = entry.getKey();
                this.clientFactory.updateTopicRouteInfoFromNameServer(topic);
            }
        }
    }

    private ConcurrentMap<String, SubscriptionData> getSubscriptionInner() {
        return this.reBalance.getSubscriptionInner();
    }

    private void checkConfig() throws ClientException {

        if (null == this.defaultPushConsumer.getConsumerGroup()) {
            throw new ClientException("consumerGroup is null", null);
        }

        if (this.defaultPushConsumer.getConsumerGroup().equals(MixAll.DEFAULT_CONSUMER_GROUP)) {
            throw new ClientException(
                    "consumerGroup can not equal "
                            + MixAll.DEFAULT_CONSUMER_GROUP
                            + ", please specify another one.",
                    null);
        }

        if (null == this.defaultPushConsumer.getMessageModel()) {
            throw new ClientException(
                    "messageModel is null",
                    null);
        }

        if (null == this.defaultPushConsumer.getConsumeFromWhere()) {
            throw new ClientException(
                    "consumeFromWhere is null",
                    null);
        }

        Date dt = UtilAll.parseDate(this.defaultPushConsumer.getConsumeTimestamp(), UtilAll.YYYYMMDDHHMMSS);
        if (null == dt) {
            throw new ClientException(
                    "consumeTimestamp is invalid, the valid format is yyyyMMddHHmmss,but received "
                            + this.defaultPushConsumer.getConsumeTimestamp(), null);
        }

        // allocateMessageQueueStrategy
        if (null == this.defaultPushConsumer.getAllocateMessageQueueStrategy()) {
            throw new ClientException(
                    "allocateMessageQueueStrategy is null", null);
        }

        // subscription
        if (null == this.defaultPushConsumer.getSubscription()) {
            throw new ClientException(
                    "subscription is null",
                    null);
        }

        // messageListener
        if (null == this.defaultPushConsumer.getMessageListener()) {
            throw new ClientException(
                    "messageListener is null",
                    null);
        }

        boolean orderly = this.defaultPushConsumer.getMessageListener() instanceof MessageListenerOrderly;
        boolean concurrently = this.defaultPushConsumer.getMessageListener() instanceof MessageListenerConcurrently;
        if (!orderly && !concurrently) {
            throw new ClientException(
                    "messageListener must be instanceof MessageListenerOrderly or MessageListenerConcurrently",
                    null);
        }

        if (this.defaultPushConsumer.getConsumeThreadMin() < 1
                || this.defaultPushConsumer.getConsumeThreadMin() > 1000) {
            throw new ClientException(
                    "consumeThreadMin Out of range [1, 1000]",
                    null);
        }

        if (this.defaultPushConsumer.getConsumeThreadMax() < 1 || this.defaultPushConsumer.getConsumeThreadMax() > 1000) {
            throw new ClientException(
                    "consumeThreadMax Out of range [1, 1000]",
                    null);
        }

        if (this.defaultPushConsumer.getConsumeThreadMin() > this.defaultPushConsumer.getConsumeThreadMax()) {
            throw new ClientException(
                    "consumeThreadMin (" + this.defaultPushConsumer.getConsumeThreadMin() + ") "
                            + "is larger than consumeThreadMax (" + this.defaultPushConsumer.getConsumeThreadMax() + ")",
                    null);
        }

        if (this.defaultPushConsumer.getConsumeConcurrentlyMaxSpan() < 1
                || this.defaultPushConsumer.getConsumeConcurrentlyMaxSpan() > 65535) {
            throw new ClientException(
                    "consumeConcurrentlyMaxSpan Out of range [1, 65535]",
                    null);
        }

        if (this.defaultPushConsumer.getPullThresholdForQueue() < 1 || this.defaultPushConsumer.getPullThresholdForQueue() > 65535) {
            throw new ClientException(
                    "pullThresholdForQueue Out of range [1, 65535]",
                    null);
        }

        if (this.defaultPushConsumer.getPullThresholdForTopic() != -1) {
            if (this.defaultPushConsumer.getPullThresholdForTopic() < 1 || this.defaultPushConsumer.getPullThresholdForTopic() > 6553500) {
                throw new ClientException(
                        "pullThresholdForTopic Out of range [1, 6553500]",
                        null);
            }
        }

        if (this.defaultPushConsumer.getPullThresholdSizeForQueue() < 1 || this.defaultPushConsumer.getPullThresholdSizeForQueue() > 1024) {
            throw new ClientException(
                    "pullThresholdSizeForQueue Out of range [1, 1024]",
                    null);
        }

        if (this.defaultPushConsumer.getPullThresholdSizeForTopic() != -1) {
            if (this.defaultPushConsumer.getPullThresholdSizeForTopic() < 1 || this.defaultPushConsumer.getPullThresholdSizeForTopic() > 102400) {
                throw new ClientException(
                        "pullThresholdSizeForTopic Out of range [1, 102400]",
                        null);
            }
        }

        if (this.defaultPushConsumer.getPullInterval() < 0 || this.defaultPushConsumer.getPullInterval() > 65535) {
            throw new ClientException(
                    "pullInterval Out of range [0, 65535]",
                    null);
        }

        if (this.defaultPushConsumer.getConsumeMessageBatchMaxSize() < 1
                || this.defaultPushConsumer.getConsumeMessageBatchMaxSize() > 1024) {
            throw new ClientException(
                    "consumeMessageBatchMaxSize Out of range [1, 1024]", null);
        }

        if (this.defaultPushConsumer.getPullBatchSize() < 1 || this.defaultPushConsumer.getPullBatchSize() > 1024) {
            throw new ClientException(
                    "pullBatchSize Out of range [1, 1024]", null);
        }
    }

    public void subscribe(String topic, String subExpression) throws ClientException {
        try {
            SubscriptionData subscriptionData = Filter.buildSubscriptionData(this.defaultPushConsumer.getConsumerGroup(),
                    topic, subExpression);
            this.reBalance.getSubscriptionInner().put(topic, subscriptionData);
            if (this.clientFactory != null) {
                this.clientFactory.sendHeartbeatToAllBrokerWithLock();
            }
        } catch (Exception e) {
            throw new ClientException("subscription exception", e);
        }
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        this.clientFactory.getAdmin().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    public long maxOffset(MessageQueue mq) throws ClientException {
        return this.clientFactory.getAdmin().maxOffset(mq);
    }

    public long minOffset(MessageQueue mq) throws ClientException {
        return this.clientFactory.getAdmin().minOffset(mq);
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        return this.clientFactory.getAdmin().earliestMsgStoreTime(mq);
    }

    public ExtMessage viewMessage(String offsetMsgId) throws RemotingException, BrokerException, InterruptedException, ClientException{
        return this.clientFactory.getAdmin().viewMessage(offsetMsgId);
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return this.clientFactory.getConsumerStatsManager();
    }

    public boolean hasHook() {
        return !this.consumeMessageHookList.isEmpty();
    }

    public void executeHookBefore(final ConsumeMessageContext context) {
        if (!this.consumeMessageHookList.isEmpty()) {
            for (ConsumeMessageHook hook : this.consumeMessageHookList) {
                try {
                    hook.consumeMessageBefore(context);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public void executeHookAfter(final ConsumeMessageContext context) {
        if (!this.consumeMessageHookList.isEmpty()) {
            for (ConsumeMessageHook hook : this.consumeMessageHookList) {
                try {
                    hook.consumeMessageAfter(context);
                } catch (Throwable ignored) {
                }
            }
        }
    }


}
