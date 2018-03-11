package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.consumer.listener.ConsumeConcurrentlyStatus;
import cn.xianyijun.wisp.client.consumer.store.LocalFileOffsetStore;
import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.client.consumer.store.RemoteBrokerOffsetStore;
import cn.xianyijun.wisp.client.hook.ConsumeMessageContext;
import cn.xianyijun.wisp.client.hook.ConsumeMessageHook;
import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.filter.Filters;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class ConsumerPullDelegate implements ConsumerInner {

    private final DefaultPullConsumer defaultPullConsumer;
    private final long consumerStartTimestamp = System.currentTimeMillis();
    private final RPCHook rpcHook;
    private final ArrayList<ConsumeMessageHook> consumeMessageHookList = new ArrayList<ConsumeMessageHook>();
    private final ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<FilterMessageHook>();
    private volatile ServiceState serviceState = ServiceState.CREATE_JUST;
    private ClientFactory clientFactory;
    private PullConsumerWrapper pullWrapper;
    private OffsetStore offsetStore;
    private AbstractReBalance reBalance = new PullReBalance(this);

    public synchronized void start() throws ClientException {
        switch (this.serviceState) {
            case CREATE_JUST:
                this.serviceState = ServiceState.START_FAILED;

                this.checkConfig();

                this.copySubscription();

                if (this.defaultPullConsumer.getMessageModel() == MessageModel.CLUSTERING) {
                    this.defaultPullConsumer.changeInstanceNameToPID();
                }

                this.clientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultPullConsumer, this.rpcHook);

                this.reBalance.setConsumerGroup(this.defaultPullConsumer.getConsumerGroup());
                this.reBalance.setMessageModel(this.defaultPullConsumer.getMessageModel());
                this.reBalance.setAllocateMessageQueueStrategy(this.defaultPullConsumer.getAllocateMessageQueueStrategy());
                this.reBalance.setClientFactory(this.clientFactory);

                this.pullWrapper = new PullConsumerWrapper(
                        clientFactory,
                        this.defaultPullConsumer.getConsumerGroup(), isUnitMode());
                this.pullWrapper.setFilterMessageHookList(filterMessageHookList);

                if (this.defaultPullConsumer.getOffsetStore() != null) {
                    this.offsetStore = this.defaultPullConsumer.getOffsetStore();
                } else {
                    switch (this.defaultPullConsumer.getMessageModel()) {
                        case BROADCASTING:
                            this.offsetStore = new LocalFileOffsetStore(this.clientFactory, this.defaultPullConsumer.getConsumerGroup());
                            break;
                        case CLUSTERING:
                            this.offsetStore = new RemoteBrokerOffsetStore(this.clientFactory, this.defaultPullConsumer.getConsumerGroup());
                            break;
                        default:
                            break;
                    }
                    this.defaultPullConsumer.setOffsetStore(this.offsetStore);
                }

                this.offsetStore.load();

                boolean registerOK = clientFactory.registerConsumer(this.defaultPullConsumer.getConsumerGroup(), this);
                if (!registerOK) {
                    this.serviceState = ServiceState.CREATE_JUST;

                    throw new ClientException("The consumer group[" + this.defaultPullConsumer.getConsumerGroup()
                            + "] has been created before, specify another name please.",
                            null);
                }

                clientFactory.start();
                log.info("the consumer [{}] start OK", this.defaultPullConsumer.getConsumerGroup());
                this.serviceState = ServiceState.RUNNING;
                break;
            case RUNNING:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
                throw new ClientException("The PullConsumer service state not OK, maybe started once, "
                        + this.serviceState,
                        null);
            default:
                break;
        }

    }

    public synchronized void shutdown() {
        switch (this.serviceState) {
            case CREATE_JUST:
                break;
            case RUNNING:
                this.persistConsumerOffset();
                this.clientFactory.unregisterConsumer(this.defaultPullConsumer.getConsumerGroup());
                this.clientFactory.shutdown();
                log.info("the consumer [{}] shutdown OK", this.defaultPullConsumer.getConsumerGroup());
                this.serviceState = ServiceState.SHUTDOWN_ALREADY;
                break;
            case SHUTDOWN_ALREADY:
                break;
            default:
                break;
        }
    }

    private void checkConfig() throws ClientException {
        if (null == this.defaultPullConsumer.getConsumerGroup()) {
            throw new ClientException(
                    "consumerGroup is null",
                    null);
        }

        if (this.defaultPullConsumer.getConsumerGroup().equals(MixAll.DEFAULT_CONSUMER_GROUP)) {
            throw new ClientException(
                    "consumerGroup can not equal "
                            + MixAll.DEFAULT_CONSUMER_GROUP
                            + ", please specify another one.",
                    null);
        }

        if (null == this.defaultPullConsumer.getMessageModel()) {
            throw new ClientException(
                    "messageModel is null",
                    null);
        }

        if (null == this.defaultPullConsumer.getAllocateMessageQueueStrategy()) {
            throw new ClientException(
                    "allocateMessageQueueStrategy is null",
                    null);
        }

        if (this.defaultPullConsumer.getConsumerTimeoutMillisWhenSuspend() < this.defaultPullConsumer.getBrokerSuspendMaxTimeMillis()) {
            throw new ClientException(
                    "Long polling mode, the consumer consumerTimeoutMillisWhenSuspend must greater than brokerSuspendMaxTimeMillis",
                    null);
        }
    }

    private void copySubscription() throws ClientException {
        try {
            Set<String> registerTopics = this.defaultPullConsumer.getRegisterTopics();
            if (registerTopics != null) {
                for (final String topic : registerTopics) {
                    SubscriptionData subscriptionData = Filters.buildSubscriptionData(this.defaultPullConsumer.getConsumerGroup(),
                            topic, SubscriptionData.SUB_ALL);
                    this.reBalance.getSubscriptionInner().put(topic, subscriptionData);
                }
            }
        } catch (Exception e) {
            throw new ClientException("subscription exception", e);
        }
    }


    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        this.makeSureStateOK();
        this.clientFactory.getAdmin().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    private void makeSureStateOK() throws ClientException {
        if (this.serviceState != ServiceState.RUNNING) {
            throw new ClientException("The consumer service state not OK, "
                    + this.serviceState,
                    null);
        }
    }

    public PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        return pull(mq, subExpression, offset, maxNums, this.defaultPullConsumer.getConsumerPullTimeoutMillis());
    }

    public PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums, long timeout)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.doPullSync(mq, subExpression, offset, maxNums, false, timeout);
    }

    private PullResult doPullSync(MessageQueue mq, String subExpression, long offset, int maxNums, boolean block,
                                    long timeout)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        this.makeSureStateOK();

        if (null == mq) {
            throw new ClientException("mq is null", null);
        }

        if (offset < 0) {
            throw new ClientException("offset < 0", null);
        }

        if (maxNums <= 0) {
            throw new ClientException("maxNums <= 0", null);
        }

        this.subscriptionAutomatically(mq.getTopic());

        int sysFlag = PullSysFlag.buildSysFlag(false, block, true, false);

        SubscriptionData subscriptionData;
        try {
            subscriptionData = Filters.buildSubscriptionData(this.defaultPullConsumer.getConsumerGroup(),
                    mq.getTopic(), subExpression);
        } catch (Exception e) {
            throw new ClientException("parse subscription error", e);
        }

        long timeoutMillis = block ? this.defaultPullConsumer.getConsumerTimeoutMillisWhenSuspend() : timeout;

        PullResult pullResult = this.pullWrapper.doPullKernel(
                mq,
                subscriptionData.getSubString(),
                0L,
                offset,
                maxNums,
                sysFlag,
                0,
                this.defaultPullConsumer.getBrokerSuspendMaxTimeMillis(),
                timeoutMillis,
                CommunicationMode.SYNC,
                null
        );
        this.pullWrapper.processPullResult(mq, pullResult, subscriptionData);
        if (!this.consumeMessageHookList.isEmpty()) {
            ConsumeMessageContext consumeMessageContext = new ConsumeMessageContext();
            consumeMessageContext.setConsumerGroup(this.groupName());
            consumeMessageContext.setMq(mq);
            consumeMessageContext.setMsgList(pullResult.getMsgFoundList());
            consumeMessageContext.setSuccess(false);
            this.executeHookBefore(consumeMessageContext);
            consumeMessageContext.setStatus(ConsumeConcurrentlyStatus.CONSUME_SUCCESS.toString());
            consumeMessageContext.setSuccess(true);
            this.executeHookAfter(consumeMessageContext);
        }
        return pullResult;
    }


    private void subscriptionAutomatically(final String topic) {
        if (!this.reBalance.getSubscriptionInner().containsKey(topic)) {
            try {
                SubscriptionData subscriptionData = Filters.buildSubscriptionData(this.defaultPullConsumer.getConsumerGroup(),
                        topic, SubscriptionData.SUB_ALL);
                this.reBalance.subscriptionInner.putIfAbsent(topic, subscriptionData);
            } catch (Exception ignore) {
            }
        }
    }

    private void executeHookBefore(final ConsumeMessageContext context) {
        if (this.consumeMessageHookList.isEmpty()) {
            return;
        }
        for (ConsumeMessageHook hook : this.consumeMessageHookList) {
            try {
                hook.consumeMessageBefore(context);
            } catch (Throwable ignored) {
            }
        }
    }

    private void executeHookAfter(final ConsumeMessageContext context) {
        if (this.consumeMessageHookList.isEmpty()) {
            return;
        }
        for (ConsumeMessageHook hook : this.consumeMessageHookList) {
            try {
                hook.consumeMessageAfter(context);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public String groupName() {
        return this.defaultPullConsumer.getConsumerGroup();
    }

    @Override
    public MessageModel messageModel() {
        return this.defaultPullConsumer.getMessageModel();
    }

    @Override
    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_ACTIVELY;
    }

    @Override
    public ConsumeWhereEnum consumeFromWhere() {
        return ConsumeWhereEnum.CONSUME_FROM_LAST_OFFSET;
    }

    @Override
    public Set<SubscriptionData> subscription() {
        Set<SubscriptionData> result = new HashSet<>();

        Set<String> topics = this.defaultPullConsumer.getRegisterTopics();
        if (topics != null) {
            synchronized (topics) {
                for (String t : topics) {
                    SubscriptionData ms = null;
                    try {
                        ms = Filters.buildSubscriptionData(this.groupName(), t, SubscriptionData.SUB_ALL);
                    } catch (Exception e) {
                        log.error("parse subscription error", e);
                    }
                    Objects.requireNonNull(ms).setSubVersion(0L);
                    result.add(ms);
                }
            }
        }
        return result;
    }

    @Override
    public void doReBalance() {
        if (this.reBalance != null) {
            this.reBalance.doReBalance(false);
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
            log.error("group: " + this.defaultPullConsumer.getConsumerGroup() + " persistConsumerOffset exception", e);
        }
    }


    @Override
    public void updateTopicSubscribeInfo(String topic, Set<MessageQueue> info) {
        Map<String, SubscriptionData> subTable = this.reBalance.getSubscriptionInner();
        if (subTable != null) {
            if (subTable.containsKey(topic)) {
                this.reBalance.getTopicSubscribeInfoTable().put(topic, info);
            }
        }
    }

    @Override
    public boolean isSubscribeTopicNeedUpdate(String topic) {
        Map<String, SubscriptionData> subTable = this.reBalance.getSubscriptionInner();
        if (subTable != null) {
            if (subTable.containsKey(topic)) {
                return !this.reBalance.topicSubscribeInfoTable.containsKey(topic);
            }
        }

        return false;
    }

    @Override
    public boolean isUnitMode() {
        return this.defaultPullConsumer.isUnitMode();
    }

    @Override
    public ConsumerRunningInfo consumerRunningInfo() {
        ConsumerRunningInfo info = new ConsumerRunningInfo();

        Properties prop = MixAll.object2Properties(this.defaultPullConsumer);
        prop.put(ConsumerRunningInfo.PROP_CONSUMER_START_TIMESTAMP, String.valueOf(this.consumerStartTimestamp));
        info.setProperties(prop);

        info.getSubscriptionSet().addAll(this.subscription());
        return info;
    }

    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().searchOffset(mq, timestamp);
    }

    public long maxOffset(MessageQueue mq) throws ClientException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().maxOffset(mq);
    }

    public long minOffset(MessageQueue mq) throws ClientException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().minOffset(mq);
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().earliestMsgStoreTime(mq);
    }

    public ExtMessage viewMessage(String msgId)
            throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().viewMessage(msgId);
    }


    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end)
            throws ClientException, InterruptedException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().queryMessage(topic, key, maxNum, begin, end);
    }


    public ExtMessage queryMessageByUniqueKey(String topic, String uniqKey)
            throws ClientException, InterruptedException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().queryMessageByUniqueKey(topic, uniqKey);
    }


    public void sendMessageBack(ExtMessage msg, int delayLevel, final String brokerName)
            throws RemotingException, BrokerException, InterruptedException, ClientException {
        sendMessageBack(msg, delayLevel, brokerName, this.defaultPullConsumer.getConsumerGroup());
    }

    public void sendMessageBack(ExtMessage msg, int delayLevel, final String brokerName, String consumerGroup)
            throws RemotingException, BrokerException, InterruptedException, ClientException {
        try {
            String brokerAddr = (null != brokerName) ? this.clientFactory.findBrokerAddressInPublish(brokerName)
                    : RemotingHelper.parseSocketAddressAddr(msg.getStoreHost());

            if (StringUtils.isBlank(consumerGroup)) {
                consumerGroup = this.defaultPullConsumer.getConsumerGroup();
            }

            this.clientFactory.getClient().consumerSendMessageBack(brokerAddr, msg, consumerGroup, delayLevel, 3000,
                    this.defaultPullConsumer.getMaxReConsumeTimes());
        } catch (Exception e) {
            log.error("sendMessageBack Exception, " + this.defaultPullConsumer.getConsumerGroup(), e);

            Message newMsg = new Message(MixAll.getRetryTopic(this.defaultPullConsumer.getConsumerGroup()), msg.getBody());
            String originMsgId = MessageAccessor.getOriginMessageId(msg);
            MessageAccessor.setOriginMessageId(newMsg, StringUtils.isBlank(originMsgId) ? msg.getMsgId() : originMsgId);
            newMsg.setFlag(msg.getFlag());
            MessageAccessor.setProperties(newMsg, msg.getProperties());
            MessageAccessor.putProperty(newMsg, MessageConst.PROPERTY_RETRY_TOPIC, msg.getTopic());
            MessageAccessor.setReConsumeTime(newMsg, String.valueOf(msg.getReConsumeTimes() + 1));
            MessageAccessor.setMaxReConsumeTimes(newMsg, String.valueOf(this.defaultPullConsumer.getMaxReConsumeTimes()));
            newMsg.setDelayTimeLevel(3 + msg.getReConsumeTimes());
            this.clientFactory.getDefaultProducer().send(newMsg);
        }
    }

}
