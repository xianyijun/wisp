package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.hook.CheckForbiddenContext;
import cn.xianyijun.wisp.client.hook.CheckForbiddenHook;
import cn.xianyijun.wisp.client.hook.SendMessageContext;
import cn.xianyijun.wisp.client.hook.SendMessageHook;
import cn.xianyijun.wisp.client.latency.FaultStrategy;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.client.producer.inner.MQProducerInner;
import cn.xianyijun.wisp.common.ClientErrorCode;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.common.message.BatchMessage;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageClientIDSetter;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageId;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.message.MessageType;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.CheckTransactionStateRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.EndTransactionRequestHeader;
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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The type Producer delegate.
 *
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class ProducerDelegate implements MQProducerInner {

    private final DefaultProducer defaultMQProducer;
    private final RPCHook rpcHook;
    private final ConcurrentMap<String, TopicPublishInfo> topicPublishInfoTable =
            new ConcurrentHashMap<>();
    private final ArrayList<SendMessageHook> sendMessageHookList = new ArrayList<>();
    private final Random random = new Random();
    private int zipCompressLevel = Integer.parseInt(System.getProperty(MixAll.MESSAGE_COMPRESS_LEVEL, "5"));
    private ServiceState serviceState = ServiceState.CREATE_JUST;
    private ClientInstance clientFactory;
    private FaultStrategy faultStrategy = new FaultStrategy();
    private ArrayList<CheckForbiddenHook> checkForbiddenHookList = new ArrayList<>();
    private ExecutorService checkExecutor;
    private BlockingQueue<Runnable> checkRequestQueue;

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

    private void checkConfig() throws ClientException {
        if (StringUtils.isEmpty(this.defaultMQProducer.getProducerGroup())) {
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

    @Override
    public void checkTransactionState(String addr, ExtMessage msg, CheckTransactionStateRequestHeader header) {
        Runnable request = new Runnable() {
            private final String brokerAddr = addr;
            private final ExtMessage message = msg;
            private final CheckTransactionStateRequestHeader checkRequestHeader = header;
            private final String group = ProducerDelegate.this.defaultMQProducer.getProducerGroup();

            @Override
            public void run() {
                TransactionCheckListener transactionCheckListener = ProducerDelegate.this.checkListener();
                if (transactionCheckListener != null) {
                    LocalTransactionState localTransactionState = LocalTransactionState.UNKNOW;
                    Throwable exception = null;
                    try {
                        localTransactionState = transactionCheckListener.checkLocalTransactionState(message);
                    } catch (Throwable e) {
                        log.error("Broker call checkTransactionState, but checkLocalTransactionState exception", e);
                        exception = e;
                    }

                    this.processTransactionState(
                            localTransactionState,
                            group,
                            exception);
                } else {
                    log.warn("checkTransactionState, pick transactionCheckListener by group[{}] failed", group);
                }
            }

            private void processTransactionState(
                    final LocalTransactionState localTransactionState,
                    final String producerGroup,
                    final Throwable exception) {
                final EndTransactionRequestHeader thisHeader = new EndTransactionRequestHeader();
                thisHeader.setCommitLogOffset(checkRequestHeader.getCommitLogOffset());
                thisHeader.setProducerGroup(producerGroup);
                thisHeader.setTranStateTableOffset(checkRequestHeader.getTranStateTableOffset());
                thisHeader.setFromTransactionCheck(true);

                String uniqueKey = message.getProperties().get(MessageConst.PROPERTY_UNIQUE_CLIENT_MESSAGE_ID_KEYIDX);
                if (uniqueKey == null) {
                    uniqueKey = message.getMsgId();
                }
                thisHeader.setMsgId(uniqueKey);
                thisHeader.setTransactionId(checkRequestHeader.getTransactionId());
                switch (localTransactionState) {
                    case COMMIT_MESSAGE:
                        thisHeader.setCommitOrRollback(MessageSysFlag.TRANSACTION_COMMIT_TYPE);
                        break;
                    case ROLLBACK_MESSAGE:
                        thisHeader.setCommitOrRollback(MessageSysFlag.TRANSACTION_ROLLBACK_TYPE);
                        log.warn("when broker check, client rollback this transaction, {}", thisHeader);
                        break;
                    case UNKNOW:
                        thisHeader.setCommitOrRollback(MessageSysFlag.TRANSACTION_NOT_TYPE);
                        log.warn("when broker check, client does not know this transaction state, {}", thisHeader);
                        break;
                    default:
                        break;
                }

                String remark = null;
                if (exception != null) {
                    remark = "checkLocalTransactionState Exception: " + RemotingHelper.exceptionSimpleDesc(exception);
                }

                try {
                    ProducerDelegate.this.clientFactory.getClient().endTransactionOneWay(brokerAddr, thisHeader, remark,
                            3000);
                } catch (Exception e) {
                    log.error("endTransactionOneWay exception", e);
                }
            }
        };
        this.checkExecutor.submit(request);
    }

    @Override
    public TransactionCheckListener checkListener() {
        if (this.defaultMQProducer instanceof TransactionProducer) {
            TransactionProducer producer = (TransactionProducer) defaultMQProducer;
            return producer.getTransactionCheckListener();
        }
        return null;
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

    public void send(Message msg,
                     SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {
        send(msg, sendCallback, this.defaultMQProducer.getSendMsgTimeout());
    }

    public void send(Message msg, SendCallback sendCallback, long timeout)
            throws ClientException, RemotingException, InterruptedException {
        try {
            this.doSend(msg, CommunicationMode.ASYNC, sendCallback, timeout);
        } catch (BrokerException e) {
            throw new ClientException("unKnown exception", e);
        }
    }

    public void sendOneWay(Message msg) throws ClientException, InterruptedException {
        try {
            this.doSend(msg, CommunicationMode.ONEWAY, null, this.defaultMQProducer.getSendMsgTimeout());
        } catch (BrokerException e) {
            throw new ClientException("unknown exception", e);
        }
    }

    public void sendOneWay(Message msg,
                           MessageQueue mq) throws ClientException, RemotingException, InterruptedException {
        this.makeSureStateOK();

        try {
            this.doKernelSend(msg, mq, CommunicationMode.ONEWAY, null, null, this.defaultMQProducer.getSendMsgTimeout());
        } catch (BrokerException e) {
            throw new ClientException("unknown exception", e);
        }
    }

    public void sendOneWay(Message msg, MessageQueueSelector selector, Object arg)
            throws ClientException, RemotingException, InterruptedException {
        try {
            this.doSendSelect(msg, selector, arg, CommunicationMode.ONEWAY, null, this.defaultMQProducer.getSendMsgTimeout());
        } catch (BrokerException e) {
            throw new ClientException("unknown exception", e);
        }
    }


    public SendResult send(Message msg, MessageQueue mq)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        return send(msg, mq, this.defaultMQProducer.getSendMsgTimeout());
    }

    public SendResult send(Message msg, MessageQueue mq, long timeout)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        this.makeSureStateOK();

        if (!msg.getTopic().equals(mq.getTopic())) {
            throw new ClientException("message's topic not equal mq's topic", null);
        }

        return this.doKernelSend(msg, mq, CommunicationMode.SYNC, null, null, timeout);
    }

    public void send(Message msg, MessageQueue mq, SendCallback sendCallback)
            throws ClientException, RemotingException, InterruptedException {
        send(msg, mq, sendCallback, this.defaultMQProducer.getSendMsgTimeout());
    }

    public void send(Message msg, MessageQueue mq, SendCallback sendCallback, long timeout)
            throws ClientException, RemotingException, InterruptedException {
        this.makeSureStateOK();

        if (!msg.getTopic().equals(mq.getTopic())) {
            throw new ClientException("message's topic not equal mq's topic", null);
        }

        try {
            this.doKernelSend(msg, mq, CommunicationMode.ASYNC, sendCallback, null, timeout);
        } catch (BrokerException e) {
            throw new ClientException("unknown exception", e);
        }
    }

    public SendResult send(Message msg, MessageQueueSelector selector, Object arg)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        return send(msg, selector, arg, this.defaultMQProducer.getSendMsgTimeout());
    }

    public SendResult send(Message msg, MessageQueueSelector selector, Object arg, long timeout)
            throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.doSendSelect(msg, selector, arg, CommunicationMode.SYNC, null, timeout);
    }

    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback)
            throws ClientException, RemotingException, InterruptedException {
        send(msg, selector, arg, sendCallback, this.defaultMQProducer.getSendMsgTimeout());
    }

    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback, long timeout)
            throws ClientException, RemotingException, InterruptedException {
        try {
            this.doSendSelect(msg, selector, arg, CommunicationMode.ASYNC, sendCallback, timeout);
        } catch (BrokerException e) {
            throw new ClientException("unknown exception", e);
        }
    }


    private SendResult doSendSelect(
            Message msg,
            MessageQueueSelector selector,
            Object arg,
            final CommunicationMode communicationMode,
            final SendCallback sendCallback, final long timeout
    ) throws ClientException, RemotingException, BrokerException, InterruptedException {
        this.makeSureStateOK();

        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());
        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            MessageQueue mq;
            try {
                mq = selector.select(topicPublishInfo.getMessageQueueList(), msg, arg);
            } catch (Throwable e) {
                throw new ClientException("select message queue throwed exception.", e);
            }

            if (mq != null) {
                return this.doKernelSend(msg, mq, communicationMode, sendCallback, null, timeout);
            } else {
                throw new ClientException("select message queue return null.", null);
            }
        }

        throw new ClientException("No route info for this topic, " + msg.getTopic(), null);
    }


    private SendResult doSend(
            Message msg,
            final CommunicationMode communicationMode,
            final SendCallback sendCallback,
            final long timeout) throws ClientException, BrokerException, InterruptedException {
        this.makeSureStateOK();

        final long invokeID = random.nextLong();
        long beginTimestampFirst = System.currentTimeMillis();
        long beginTimestampPrev = beginTimestampFirst;
        long endTimestamp;

        TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());

        if (topicPublishInfo != null && topicPublishInfo.ok()) {
            MessageQueue messageQueue = null;
            Exception exception = null;
            SendResult sendResult = null;
            int timesTotal = communicationMode == CommunicationMode.SYNC ? 1 + this.defaultMQProducer.getRetryTimesWhenSendFailed() : 1;
            int times = 0;
            String[] brokersSent = new String[timesTotal];
            for (; times < timesTotal; times++) {
                String lastBrokerName = null == messageQueue ? null : messageQueue.getBrokerName();
                MessageQueue mqSelected = this.selectOneMessageQueue(topicPublishInfo, lastBrokerName);
                if (mqSelected != null) {
                    messageQueue = mqSelected;
                    brokersSent[times] = messageQueue.getBrokerName();
                    try {
                        beginTimestampPrev = System.currentTimeMillis();
                        sendResult = this.doKernelSend(msg, messageQueue, communicationMode, sendCallback, topicPublishInfo, timeout);
                        endTimestamp = System.currentTimeMillis();
                        this.updateFaultItem(messageQueue.getBrokerName(), endTimestamp - beginTimestampPrev, false);
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
                        this.updateFaultItem(messageQueue.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                        log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, messageQueue), e);
                        log.warn(msg.toString());
                        exception = e;
                    } catch (BrokerException e) {
                        endTimestamp = System.currentTimeMillis();
                        this.updateFaultItem(messageQueue.getBrokerName(), endTimestamp - beginTimestampPrev, true);
                        log.warn(String.format("sendKernelImpl exception, resend at once, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, messageQueue), e);
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
                        this.updateFaultItem(messageQueue.getBrokerName(), endTimestamp - beginTimestampPrev, false);
                        log.warn(String.format("sendKernelImpl exception, throw exception, InvokeID: %s, RT: %sms, Broker: %s", invokeID, endTimestamp - beginTimestampPrev, messageQueue), e);
                        log.warn(msg.toString());

                        log.warn("sendKernelImpl exception", e);
                        log.warn(msg.toString());
                        throw e;
                    }
                } else {
                    break;
                }
            }
            log.info("[doSend] send success , result :{}", sendResult);
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
                    "No name server address, please set it.", null).setResponseCode(ClientErrorCode.NO_NAME_SERVER_EXCEPTION);
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
        log.info("[ProduceDelegate] doKernelSend , msg：{} ，mq :{} , topicPublishInfo :{}", msg, mq, topicPublishInfo);
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

                    String maxReConsumeTimes = MessageAccessor.getMaxReConsumeTimes(msg);
                    if (maxReConsumeTimes != null) {
                        requestHeader.setMaxReConsumeTimes(Integer.valueOf(maxReConsumeTimes));
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
    private boolean hasCheckForbiddenHook() {
        return !checkForbiddenHookList.isEmpty();
    }


    /**
     * Has send message hook boolean.
     *
     * @return the boolean
     */
    private boolean hasSendMessageHook() {
        return !this.sendMessageHookList.isEmpty();
    }


    /**
     * Execute check forbidden hook.
     *
     * @param context the context
     * @throws ClientException the client exception
     */
    private void executeCheckForbiddenHook(final CheckForbiddenContext context) throws ClientException {
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
    private void executeSendMessageHookBefore(final SendMessageContext context) {
        if (this.sendMessageHookList.isEmpty()) {
            return;
        }
        for (SendMessageHook hook : this.sendMessageHookList) {
            try {
                hook.sendMessageBefore(context);
            } catch (Throwable e) {
                log.warn("failed to executeSendMessageHookBefore", e);
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

    public void initTransactionEnv() {
        TransactionProducer producer = (TransactionProducer) this.defaultMQProducer;
        this.checkRequestQueue = new LinkedBlockingQueue<>(producer.getCheckRequestHoldMax());
        this.checkExecutor = new ThreadPoolExecutor(
                producer.getCheckThreadPoolMinSize(),
                producer.getCheckThreadPoolMaxSize(),
                1000 * 60,
                TimeUnit.MILLISECONDS,
                this.checkRequestQueue, new WispThreadFactory("CheckThread_"));
    }

    public void destroyTransactionEnv() {
        this.checkExecutor.shutdown();
        this.checkRequestQueue.clear();
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

    public ExtMessage viewMessage(
            String msgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
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

    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        this.makeSureStateOK();
        this.clientFactory.getAdmin().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    public TransactionSendResult sendMessageInTransaction(Message message, LocalTransactionExecutor executor, Object arg) throws ClientException {
        if (null == executor) {
            throw new ClientException("tranExecutor is null", null);
        }

        SendResult sendResult;
        MessageAccessor.putProperty(message, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(message, MessageConst.PROPERTY_PRODUCER_GROUP, this.defaultMQProducer.getProducerGroup());
        try {
            sendResult = this.send(message);
        } catch (Exception e) {
            throw new ClientException("send message Exception", e);
        }

        LocalTransactionState localTransactionState = LocalTransactionState.UNKNOW;
        Throwable localException = null;
        switch (sendResult.getSendStatus()) {
            case SEND_OK: {
                try {
                    if (sendResult.getTransactionId() != null) {
                        message.putUserProperty("__transactionId__", sendResult.getTransactionId());
                    }
                    localTransactionState = executor.executeLocalTransactionBranch(message, arg);
                    if (null == localTransactionState) {
                        localTransactionState = LocalTransactionState.UNKNOW;
                    }

                    if (localTransactionState != LocalTransactionState.COMMIT_MESSAGE) {
                        log.info("executeLocalTransactionBranch return {}", localTransactionState);
                        log.info(message.toString());
                    }
                } catch (Throwable e) {
                    log.info("executeLocalTransactionBranch exception", e);
                    log.info(message.toString());
                    localException = e;
                }
            }
            break;
            case FLUSH_DISK_TIMEOUT:
            case FLUSH_SLAVE_TIMEOUT:
            case SLAVE_NOT_AVAILABLE:
                localTransactionState = LocalTransactionState.ROLLBACK_MESSAGE;
                break;
            default:
                break;
        }

        try {
            this.endTransaction(sendResult, localTransactionState, localException);
        } catch (Exception e) {
            log.warn("local transaction execute " + localTransactionState + ", but end broker transaction failed", e);
        }

        TransactionSendResult transactionSendResult = new TransactionSendResult();
        transactionSendResult.setSendStatus(sendResult.getSendStatus());
        transactionSendResult.setMessageQueue(sendResult.getMessageQueue());
        transactionSendResult.setMsgId(sendResult.getMsgId());
        transactionSendResult.setQueueOffset(sendResult.getQueueOffset());
        transactionSendResult.setTransactionId(sendResult.getTransactionId());
        transactionSendResult.setLocalTransactionState(localTransactionState);
        return transactionSendResult;
    }

    private void endTransaction(
            final SendResult sendResult,
            final LocalTransactionState localTransactionState,
            final Throwable localException) throws RemotingException, InterruptedException, UnknownHostException {
        final MessageId id;
        if (sendResult.getOffsetMsgId() != null) {
            id = MessageDecoder.decodeMessageId(sendResult.getOffsetMsgId());
        } else {
            id = MessageDecoder.decodeMessageId(sendResult.getMsgId());
        }
        String transactionId = sendResult.getTransactionId();
        final String brokerAddr = this.clientFactory.findBrokerAddressInPublish(sendResult.getMessageQueue().getBrokerName());
        EndTransactionRequestHeader requestHeader = new EndTransactionRequestHeader();
        requestHeader.setTransactionId(transactionId);
        requestHeader.setCommitLogOffset(id.getOffset());
        switch (localTransactionState) {
            case COMMIT_MESSAGE:
                requestHeader.setCommitOrRollback(MessageSysFlag.TRANSACTION_COMMIT_TYPE);
                break;
            case ROLLBACK_MESSAGE:
                requestHeader.setCommitOrRollback(MessageSysFlag.TRANSACTION_ROLLBACK_TYPE);
                break;
            case UNKNOW:
                requestHeader.setCommitOrRollback(MessageSysFlag.TRANSACTION_NOT_TYPE);
                break;
            default:
                break;
        }

        requestHeader.setProducerGroup(this.defaultMQProducer.getProducerGroup());
        requestHeader.setTranStateTableOffset(sendResult.getQueueOffset());
        requestHeader.setMsgId(sendResult.getMsgId());
        String remark = localException != null ? ("executeLocalTransactionBranch exception: " + localException.toString()) : null;
        this.clientFactory.getClient().endTransactionOneWay(brokerAddr, requestHeader, remark,
                this.defaultMQProducer.getSendMsgTimeout());
    }

}
