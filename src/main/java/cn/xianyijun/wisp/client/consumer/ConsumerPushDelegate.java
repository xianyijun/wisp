package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.consumer.listener.ConsumeMessageConcurrentlyService;
import cn.xianyijun.wisp.client.consumer.listener.ConsumeMessageOrderlyService;
import cn.xianyijun.wisp.client.consumer.listener.MessageListener;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerConcurrently;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerOrderly;
import cn.xianyijun.wisp.client.consumer.store.LocalFileOffsetStore;
import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.client.consumer.store.RemoteBrokerOffsetStore;
import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.filter.Filter;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class ConsumerPushDelegate implements ConsumerInner {

    private final DefaultPushConsumer defaultPushConsumer;

    private final RPCHook rpcHook;

    private ConsumeMessageService consumeMessageService;

    private AbstractReBalance reBalance = new PushReBalance(this);

    private OffsetStore offsetStore;

    private volatile boolean pause = false;

    private boolean consumeOrderly = false;

    private volatile ServiceState serviceState = ServiceState.CREATE_JUST;

    private ClientInstance clientFactory;

    private PullConsumerWrapper pullConsumerWrapper;

    private final ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<>();

    private MessageListener messageListenerInner;

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
        return null;
    }

    @Override
    public ConsumeType consumeType() {
        return null;
    }

    @Override
    public ConsumeWhereEnum consumeFromWhere() {
        return null;
    }

    @Override
    public Set<SubscriptionData> subScriptions() {
        return null;
    }

    @Override
    public void doReBalance() {

    }

    @Override
    public void persistConsumerOffset() {

    }

    @Override
    public void updateTopicSubscribeInfo(String topic, Set<MessageQueue> info) {

    }

    @Override
    public boolean isSubscribeTopicNeedUpdate(String topic) {
        return false;
    }

    @Override
    public boolean isUnitMode() {
        return false;
    }

    @Override
    public ConsumerRunningInfo consumerRunningInfo() {
        return null;
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

        // consumeThreadMin
        if (this.defaultPushConsumer.getConsumeThreadMin() < 1
                || this.defaultPushConsumer.getConsumeThreadMin() > 1000) {
            throw new ClientException(
                    "consumeThreadMin Out of range [1, 1000]",
                    null);
        }

        // consumeThreadMax
        if (this.defaultPushConsumer.getConsumeThreadMax() < 1 || this.defaultPushConsumer.getConsumeThreadMax() > 1000) {
            throw new ClientException(
                    "consumeThreadMax Out of range [1, 1000]",
                    null);
        }

        // consumeThreadMin can't be larger than consumeThreadMax
        if (this.defaultPushConsumer.getConsumeThreadMin() > this.defaultPushConsumer.getConsumeThreadMax()) {
            throw new ClientException(
                    "consumeThreadMin (" + this.defaultPushConsumer.getConsumeThreadMin() + ") "
                            + "is larger than consumeThreadMax (" + this.defaultPushConsumer.getConsumeThreadMax() + ")",
                    null);
        }

        // consumeConcurrentlyMaxSpan
        if (this.defaultPushConsumer.getConsumeConcurrentlyMaxSpan() < 1
                || this.defaultPushConsumer.getConsumeConcurrentlyMaxSpan() > 65535) {
            throw new ClientException(
                    "consumeConcurrentlyMaxSpan Out of range [1, 65535]",
                    null);
        }

        // pullThresholdForQueue
        if (this.defaultPushConsumer.getPullThresholdForQueue() < 1 || this.defaultPushConsumer.getPullThresholdForQueue() > 65535) {
            throw new ClientException(
                    "pullThresholdForQueue Out of range [1, 65535]",
                    null);
        }

        // pullThresholdForTopic
        if (this.defaultPushConsumer.getPullThresholdForTopic() != -1) {
            if (this.defaultPushConsumer.getPullThresholdForTopic() < 1 || this.defaultPushConsumer.getPullThresholdForTopic() > 6553500) {
                throw new ClientException(
                        "pullThresholdForTopic Out of range [1, 6553500]",
                        null);
            }
        }

        // pullThresholdSizeForQueue
        if (this.defaultPushConsumer.getPullThresholdSizeForQueue() < 1 || this.defaultPushConsumer.getPullThresholdSizeForQueue() > 1024) {
            throw new ClientException(
                    "pullThresholdSizeForQueue Out of range [1, 1024]",
                    null);
        }

        if (this.defaultPushConsumer.getPullThresholdSizeForTopic() != -1) {
            // pullThresholdSizeForTopic
            if (this.defaultPushConsumer.getPullThresholdSizeForTopic() < 1 || this.defaultPushConsumer.getPullThresholdSizeForTopic() > 102400) {
                throw new ClientException(
                        "pullThresholdSizeForTopic Out of range [1, 102400]",
                        null);
            }
        }

        // pullInterval
        if (this.defaultPushConsumer.getPullInterval() < 0 || this.defaultPushConsumer.getPullInterval() > 65535) {
            throw new ClientException(
                    "pullInterval Out of range [0, 65535]",
                    null);
        }

        // consumeMessageBatchMaxSize
        if (this.defaultPushConsumer.getConsumeMessageBatchMaxSize() < 1
                || this.defaultPushConsumer.getConsumeMessageBatchMaxSize() > 1024) {
            throw new ClientException(
                    "consumeMessageBatchMaxSize Out of range [1, 1024]", null);
        }

        // pullBatchSize
        if (this.defaultPushConsumer.getPullBatchSize() < 1 || this.defaultPushConsumer.getPullBatchSize() > 1024) {
            throw new ClientException(
                    "pullBatchSize Out of range [1, 1024]", null);
        }
    }

}
