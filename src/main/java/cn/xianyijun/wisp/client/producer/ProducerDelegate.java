package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.hook.CheckForbiddenContext;
import cn.xianyijun.wisp.client.hook.CheckForbiddenHook;
import cn.xianyijun.wisp.client.hook.SendMessageContext;
import cn.xianyijun.wisp.client.hook.SendMessageHook;
import cn.xianyijun.wisp.client.latency.FaultStrategy;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.client.producer.inner.MQProducerInner;
import cn.xianyijun.wisp.common.ClientErrorCode;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.message.BatchMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageClientIDSetter;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.message.MessageType;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageRequestHeader;
import cn.xianyijun.wisp.common.sysflag.MessageSysFlag;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type Producer delegate.
 *
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class ProducerDelegate implements MQProducerInner {

    private int zipCompressLevel = Integer.parseInt(System.getProperty(MixAll.MESSAGE_COMPRESS_LEVEL, "5"));

    private final DefaultProducer defaultMQProducer;

    private final RPCHook rpcHook;

    private ServiceState serviceState = ServiceState.CREATE_JUST;

    private ClientInstance clientFactory;

    private FaultStrategy faultStrategy = new FaultStrategy();

    private final ConcurrentMap<String, TopicPublishInfo> topicPublishInfoTable =
            new ConcurrentHashMap<>();

    private final ArrayList<SendMessageHook> sendMessageHookList = new ArrayList<>();

    private ArrayList<CheckForbiddenHook> checkForbiddenHookList = new ArrayList<>();


    private final Random random = new Random();

    /**
     * Instantiates a new Producer delegate.
     *
     * @param defaultMQProducer the default mq producer
     */
    public ProducerDelegate(DefaultProducer defaultMQProducer) {
        this(defaultMQProducer, null);
    }

    /**
     * Start.
     *
     * @throws ClientException the client exception
     */
    public void start() throws ClientException {
        this.start(true);
    }

    /**
     * Start.
     *
     * @param startFactory the start factory
     * @throws ClientException the client exception
     */
    public void start(boolean startFactory) throws ClientException {
        switch (this.serviceState) {
            case CREATE_JUST:
                this.serviceState = ServiceState.START_FAILED;
                this.checkConfig();
                if (!this.defaultMQProducer.getProducerGroup().equals(MixAll.CLIENT_INNER_PRODUCER_GROUP)) {
                    this.defaultMQProducer.changeInstanceNameToPID();
                }
                this.serviceState = ServiceState.RUNNING;

                this.clientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.defaultMQProducer, rpcHook);
                boolean registerOK = clientFactory.registerProducer(this.defaultMQProducer.getProducerGroup(), this);

                if (!registerOK) {
                    this.serviceState = ServiceState.CREATE_JUST;
                    throw new ClientException("The producer group[" + this.defaultMQProducer.getProducerGroup()
                            + "] has been created before, specify another name please.",
                            null);
                }
                this.topicPublishInfoTable.put(this.defaultMQProducer.getCreateTopicKey(), new TopicPublishInfo());

                if (startFactory) {
                    clientFactory.start();
                }

                break;
            case RUNNING:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
                throw new ClientException("The producer service state not OK, maybe started once, " + this.serviceState, null);
            default:
                break;
        }

        this.clientFactory.sendHeartbeatToAllBrokerWithLock();
    }

    private void checkConfig() throws ClientException{
        if (StringUtils.isEmpty(this.defaultMQProducer.getProducerGroup())){
            throw new ClientException("producerGroup is null", null);
        }
        if (this.defaultMQProducer.getProducerGroup().equals(MixAll.DEFAULT_PRODUCER_GROUP)) {
            throw new ClientException("producerGroup can not equal " + MixAll.DEFAULT_PRODUCER_GROUP + ", please specify another one.",
                    null);
        }
    }

    @Override
    public Set<String> getPublishTopicList() {
        return new HashSet<>(this.topicPublishInfoTable.keySet());
    }

    @Override
    public boolean isPublishTopicNeedUpdate(String topic) {
        TopicPublishInfo prev = this.topicPublishInfoTable.get(topic);
        return null == prev || !prev.ok();
    }

    @Override
    public void updateTopicPublishInfo(String topic, TopicPublishInfo info) {
        if (info != null && topic != null) {
            TopicPublishInfo prev = this.topicPublishInfoTable.put(topic, info);
            if (prev != null) {
                log.info("updateTopicPublishInfo prev is not null, " + prev.toString());
            }
        }
    }

    @Override
    public boolean isUnitMode() {
        return this.defaultMQProducer.isUnitMode();
    }

    /**
     * Send send result.
     *
     * @param msg the msg
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    public SendResult send(
            Message msg) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return send(msg, this.defaultMQProducer.getSendMsgTimeout());
    }

    /**
     * Send send result.
     *
     * @param msg     the msg
     * @param timeout the timeout
     * @return the send result
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     */
    public SendResult send(Message msg,
                           long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.doSend(msg, CommunicationMode.SYNC, null, timeout);
    }

    private SendResult doSend(
            Message msg,
            final CommunicationMode communicationMode,
            final SendCallback sendCallback,
            final long timeout)  throws ClientException, RemotingException, BrokerException, InterruptedException {
        this.makeSureStateOK();

        final long invokeID = random.nextLong();
        long beginTimestampFirst = System.currentTimeMillis();
        long beginTimestampPrev = beginTimestampFirst;
        long endTimestamp;

        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());

        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            MessageQueue mq = null;
            Exception exception = null;
            SendResult sendResult = null;
            int timesTotal = communicationMode == CommunicationMode.SYNC ? 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed() : 1;
            int times = 0;
            String[] brokersSent = new String[timesTotal];
            for (; times < timesTotal; times++) {
                String lastBrokerName = null == mq ? null : mq.getBrokerName();
                MessageQueue mqSelected = this.selectOneMessageQueue(topicPublishInfo, lastBrokerName);
                if (mqSelected != null) {
                    mq = mqSelected;
                    brokersSent[times] = mq.getBrokerName();
                    try {
                        beginTimestampPrev = System.currentTimeMillis();
                        sendResult = this.doKernelSend(msg, mq, communicationMode, sendCallback, topicPublishInfo, timeout);
                        endTimestamp = System.currentTimeMillis();
                        this.updateFaultItem(mq.getBrokerName(), endTimestamp - beginTimestampPrev, false);
                        switch (communicationMode) {
                            case ASYNC:
                                return null;
                            case ONEWAY:
                                return null;
                            case SYNC:
                                if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                                    if (this.defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()) {
                                        continue;
                                    }
                                }

                                return sendResult;
                            default:
                                break;
                        }
                    } catch (RemotingException | ClientException e) {
                        endTimestamp = System.currentTimeMillis();
                        this.updateFaultItem(mq.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                        log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mq), e);
                        log.warn(msg.toString());
                        exception = e;
                    } catch (BrokerException e) {
                        endTimestamp = System.currentTimeMillis();
                        this.updateFaultItem(mq.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                        log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mq), e);
                        log.warn(msg.toString());
                        exception = e;
                        switch (e.getResponseCode()) {
                            case ResponseCode.TOPIC_NOT_EXIST:
                            case ResponseCode.SERVICE_NOT_AVAILABLE:
                            case ResponseCode.SYSTEM_ERROR:
                            case ResponseCode.NO_PERMISSION:
                            case ResponseCode.NO_BUYER_ID:
                            case ResponseCode.NOT_IN_CURRENT_UNIT:
                                continue;
                            default:
                                if (sendResult != null) {
                                    return sendResult;
                                }

                                throw e;
                        }
                    } catch (InterruptedException e) {
                        endTimestamp = System.currentTimeMillis();
                        this.updateFaultItem(mq.getBrokerName(), endTimestamp - beginTimestampPrev, false);
                        log.warn(String.format("sendKernelImpl exception, throw exception, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, mq), e);
                        log.warn(msg.toString());

                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        throw e;
                    }
                } else {
                    break;
                }
            }

            if (sendResult != null) {
                return sendResult;
            }

            String info = String.format("Send [%d] times, still failed, cost [%d]ms, Topic: %s, BrokersSent: %s",
                    times,
                    System.currentTimeMillis() - beginTimestampFirst,
                    msg.getTopic(),
                    Arrays.toString(brokersSent));

            ClientException mqClientException = new ClientException(info, exception);
            if (exception instanceof BrokerException) {
                mqClientException.setResponseCode(((BrokerException) exception).getResponseCode());
            } else if (exception instanceof RemotingConnectException) {
                mqClientException.setResponseCode(ClientErrorCode.CONNECT_BROKER_EXCEPTION);
            } else if (exception instanceof RemotingTimeoutException) {
                mqClientException.setResponseCode(ClientErrorCode.ACCESS_BROKER_TIMEOUT);
            } else if (exception instanceof ClientException) {
                mqClientException.setResponseCode(ClientErrorCode.BROKER_NOT_EXIST_EXCEPTION);
            }

            throw mqClientException;
        }

        List<String> nsList = this.clientFactory.getClient().getNameServerAddressList();
        if (null == nsList || nsList.isEmpty()) {
            throw new ClientException(
                    "No name server address, please set it." , null).setResponseCode(ClientErrorCode.NO_NAME_SERVER_EXCEPTION);
        }

        throw new ClientException("No route info of this topic, " + msg.getTopic(),
                null).setResponseCode(ClientErrorCode.NOT_FOUND_TOPIC_EXCEPTION);
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        this.shutdown(true);
    }


    /**
     * Shutdown.
     *
     * @param shutdownFactory the shutdown factory
     */
    public void shutdown(final boolean shutdownFactory) {
        switch (this.serviceState) {
            case CREATE_JUST:
                break;
            case RUNNING:
                this.clientFactory.unregisterProducer(this.defaultMQProducer.getProducerGroup());
                if (shutdownFactory) {
                    this.clientFactory.shutdown();
                }

                log.info("the producer [{}] shutdown OK", this.defaultMQProducer.getProducerGroup());
                this.serviceState = ServiceState.SHUTDOWN_ALREADY;
                break;
            case SHUTDOWN_ALREADY:
                break;
            default:
                break;
        }
    }


    /**
     * Fetch publish message queues list.
     *
     * @param topic the topic
     * @return the list
     * @throws ClientException the client exception
     */
    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws ClientException {
        this.makeSureStateOK();
        return this.clientFactory.getAdmin().fetchPublishMessageQueues(topic);
    }

    private void makeSureStateOK() throws ClientException {
        if (this.serviceState != ServiceState.RUNNING) {
            throw new ClientException("The producer service state not OK, "
                    + this.serviceState,
                    null);
        }
    }
    private TopicPublishInfo tryToFindTopicPublishInfo(final String topic) {
        TopicPublishInfo topicPublishInfo = this.topicPublishInfoTable.get(topic);
        if (null == topicPublishInfo || !topicPublishInfo.ok()) {
            this.topicPublishInfoTable.putIfAbsent(topic, new TopicPublishInfo());
            this.clientFactory.updateTopicRouteInfoFromNameServer(topic);
            topicPublishInfo = this.topicPublishInfoTable.get(topic);
        }

        if (topicPublishInfo.isHaveTopicRouterInfo() || topicPublishInfo.ok()) {
            return topicPublishInfo;
        } else {
            this.clientFactory.updateTopicRouteInfoFromNameServer(topic, true, this.defaultMQProducer);
            topicPublishInfo = this.topicPublishInfoTable.get(topic);
            return topicPublishInfo;
        }
    }


    /**
     * Update fault item.
     *
     * @param brokerName     the broker name
     * @param currentLatency the current latency
     * @param isolation      the isolation
     */
    public void updateFaultItem(final String brokerName, final long currentLatency, boolean isolation) {
        this.faultStrategy.updateFaultItem(brokerName, currentLatency, isolation);
    }


    /**
     * Select one message queue message queue.
     *
     * @param tpInfo         the tp info
     * @param lastBrokerName the last broker name
     * @return the message queue
     */
    public MessageQueue selectOneMessageQueue(final TopicPublishInfo tpInfo, final String lastBrokerName) {
        return this.faultStrategy.selectOneMessageQueue(tpInfo, lastBrokerName);
    }

    private SendResult doKernelSend(final Message msg,
                                      final MessageQueue mq,
                                      final CommunicationMode communicationMode,
                                      final SendCallback sendCallback,
                                      final TopicPublishInfo topicPublishInfo,
                                      final long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        String brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            tryToFindTopicPublishInfo(mq.getTopic());
            brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }

        SendMessageContext context = null;
        if (brokerAddr != null) {
            brokerAddr = MixAll.brokerVIPChannel(this.defaultMQProducer.isSendMessageWithVIPChannel(), brokerAddr);

            byte[] prevBody = msg.getBody();
            try {
                //for MessageBatch,ID has been set in the generating process
                if (!(msg instanceof BatchMessage)) {
                    MessageClientIDSetter.setUniqueID(msg);
                }

                int sysFlag = 0;
                if (this.tryToCompressMessage(msg)) {
                    sysFlag |= MessageSysFlag.COMPRESSED_FLAG;
                }

                final String tranMsg = msg.getProperty(MessageConst.PROPERTY_TRANSACTION_PREPARED);
                if (tranMsg != null && Boolean.parseBoolean(tranMsg)) {
                    sysFlag |= MessageSysFlag.TRANSACTION_PREPARED_TYPE;
                }

                if (hasCheckForbiddenHook()) {
                    CheckForbiddenContext checkForbiddenContext = new CheckForbiddenContext();
                    checkForbiddenContext.setNameServerAddr(this.defaultMQProducer.getNameServerAddr());
                    checkForbiddenContext.setGroup(this.defaultMQProducer.getProducerGroup());
                    checkForbiddenContext.setCommunicationMode(communicationMode);
                    checkForbiddenContext.setBrokerAddr(brokerAddr);
                    checkForbiddenContext.setMessage(msg);
                    checkForbiddenContext.setMq(mq);
                    checkForbiddenContext.setUnitMode(this.isUnitMode());
                    this.executeCheckForbiddenHook(checkForbiddenContext);
                }

                if (this.hasSendMessageHook()) {
                    context = new SendMessageContext();
                    context.setProducer(this);
                    context.setProducerGroup(this.defaultMQProducer.getProducerGroup());
                    context.setCommunicationMode(communicationMode);
                    context.setBornHost(this.defaultMQProducer.getClientIP());
                    context.setBrokerAddr(brokerAddr);
                    context.setMessage(msg);
                    context.setMq(mq);
                    String isTrans = msg.getProperty(MessageConst.PROPERTY_TRANSACTION_PREPARED);
                    if (isTrans != null && "true".equals(isTrans)) {
                        context.setMsgType(MessageType.TRANS_MSG_HALF);
                    }

                    if (msg.getProperty("__STARTDELIVERTIME") != null || msg.getProperty(MessageConst.PROPERTY_DELAY_TIME_LEVEL) != null) {
                        context.setMsgType(MessageType.DELAY_MSG);
                    }
                    this.executeSendMessageHookBefore(context);
                }

                ProduceMessageRequestHeader requestHeader = new ProduceMessageRequestHeader();
                requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
                requestHeader.setTopic(msg.getTopic());
                requestHeader.setDefaultTopic(this.defaultMQProducer.getCreateTopicKey());
                requestHeader.setDefaultTopicQueueNums(this.defaultMQProducer.getDefaultTopicQueueNums());
                requestHeader.setQueueId(mq.getQueueId());
                requestHeader.setSysFlag(sysFlag);
                requestHeader.setBornTimestamp(System.currentTimeMillis());
                requestHeader.setFlag(msg.getFlag());
                requestHeader.setProperties(MessageDecoder.messageProperties2String(msg.getProperties()));
                requestHeader.setReConsumeTimes(0);
                requestHeader.setUnitMode(this.isUnitMode());
                requestHeader.setBatch(msg instanceof BatchMessage);
                if (requestHeader.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    String reConsumeTimes = MessageAccessor.getReConsumeTime(msg);
                    if (reConsumeTimes != null) {
                        requestHeader.setReConsumeTimes(Integer.valueOf(reConsumeTimes));
                        MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_RECONSUME_TIME);
                    }

                    String maxReconsumeTimes = MessageAccessor.getMaxReConsumeTimes(msg);
                    if (maxReconsumeTimes != null) {
                        requestHeader.setMaxReConsumeTimes(Integer.valueOf(maxReconsumeTimes));
                        MessageAccessor.clearProperty(msg, MessageConst.PROPERTY_MAX_RECONSUME_TIMES);
                    }
                }

                SendResult sendResult = null;
                switch (communicationMode) {
                    case ASYNC:
                        sendResult = this.clientFactory.getClient().sendMessage(
                                brokerAddr,
                                mq.getBrokerName(),
                                msg,
                                requestHeader,
                                timeout,
                                communicationMode,
                                sendCallback,
                                topicPublishInfo,
                                this.clientFactory,
                                this.defaultMQProducer.getRetryTimesWhenSendAsyncFailed(),
                                context,
                                this);
                        break;
                    case ONEWAY:
                    case SYNC:
                        sendResult = this.clientFactory.getClient().sendMessage(
                                brokerAddr,
                                mq.getBrokerName(),
                                msg,
                                requestHeader,
                                timeout,
                                communicationMode,
                                context,
                                this);
                        break;
                    default:
                        assert false;
                        break;
                }

                if (this.hasSendMessageHook()) {
                    context.setSendResult(sendResult);
                    this.executeSendMessageHookAfter(context);
                }

                return sendResult;
            } catch (RemotingException | InterruptedException | BrokerException e) {
                if (this.hasSendMessageHook()) {
                    context.setException(e);
                    this.executeSendMessageHookAfter(context);
                }
                throw e;
            } finally {
                msg.setBody(prevBody);
            }
        }

        throw new ClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }

    private boolean tryToCompressMessage(final Message msg) {
        if (msg instanceof BatchMessage) {
            //batch dose not support compressing right now
            return false;
        }
        byte[] body = msg.getBody();
        if (body != null) {
            if (body.length >= this.defaultMQProducer.getCompressMsgBodyOverHowMuch()) {
                try {
                    byte[] data = UtilAll.compress(body, zipCompressLevel);
                    if (data != null) {
                        msg.setBody(data);
                        return true;
                    }
                } catch (IOException e) {
                    log.error("tryToCompressMessage exception", e);
                    log.warn(msg.toString());
                }
            }
        }

        return false;
    }


    /**
     * Has check forbidden hook boolean.
     *
     * @return the boolean
     */
    public boolean hasCheckForbiddenHook() {
        return !checkForbiddenHookList.isEmpty();
    }


    /**
     * Has send message hook boolean.
     *
     * @return the boolean
     */
    public boolean hasSendMessageHook() {
        return !this.sendMessageHookList.isEmpty();
    }


    /**
     * Execute check forbidden hook.
     *
     * @param context the context
     * @throws ClientException the client exception
     */
    public void executeCheckForbiddenHook(final CheckForbiddenContext context) throws ClientException {
        if (hasCheckForbiddenHook()) {
            for (CheckForbiddenHook hook : checkForbiddenHookList) {
                hook.checkForbidden(context);
            }
        }
    }

    /**
     * Execute send message hook before.
     *
     * @param context the context
     */
    public void executeSendMessageHookBefore(final SendMessageContext context) {
        if (!this.sendMessageHookList.isEmpty()) {
            for (SendMessageHook hook : this.sendMessageHookList) {
                try {
                    hook.sendMessageBefore(context);
                } catch (Throwable e) {
                    log.warn("failed to executeSendMessageHookBefore", e);
                }
            }
        }
    }

    /**
     * Execute send message hook after.
     *
     * @param context the context
     */
    public void executeSendMessageHookAfter(final SendMessageContext context) {
        if (!this.sendMessageHookList.isEmpty()) {
            for (SendMessageHook hook : this.sendMessageHookList) {
                try {
                    hook.sendMessageAfter(context);
                } catch (Throwable e) {
                    log.warn("failed to executeSendMessageHookAfter", e);
                }
            }
        }
    }

}
