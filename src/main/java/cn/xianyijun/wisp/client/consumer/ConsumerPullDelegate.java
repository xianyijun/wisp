package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.broker.mqtrace.ConsumeMessageHook;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.filter.Filter;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
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
    private PullConsumerWrapper pullAPIWrapper;
    private OffsetStore offsetStore;
    private AbstractReBalance reBalance = new PullReBalance(this);

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
        Set<SubscriptionData> result = new HashSet<SubscriptionData>();

        Set<String> topics = this.defaultPullConsumer.getRegisterTopics();
        if (topics != null) {
            synchronized (topics) {
                for (String t : topics) {
                    SubscriptionData ms = null;
                    try {
                        ms = Filter.buildSubscriptionData(this.groupName(), t, SubscriptionData.SUB_ALL);
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
