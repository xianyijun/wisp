package cn.xianyijun.wisp.client;

import cn.xianyijun.wisp.client.consumer.ExtPullResult;
import cn.xianyijun.wisp.client.consumer.PullCallback;
import cn.xianyijun.wisp.client.consumer.PullResult;
import cn.xianyijun.wisp.client.consumer.PullStatus;
import cn.xianyijun.wisp.client.hook.SendMessageContext;
import cn.xianyijun.wisp.client.producer.ProducerDelegate;
import cn.xianyijun.wisp.client.producer.SendCallback;
import cn.xianyijun.wisp.client.producer.SendResult;
import cn.xianyijun.wisp.client.producer.SendStatus;
import cn.xianyijun.wisp.client.producer.TopicPublishInfo;
import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.admin.ConsumeStats;
import cn.xianyijun.wisp.common.admin.TopicStatsTable;
import cn.xianyijun.wisp.common.message.BatchMessage;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageClientIDSetter;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.namesrv.TopAddressing;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.body.BrokerStatsData;
import cn.xianyijun.wisp.common.protocol.body.CheckClientRequestBody;
import cn.xianyijun.wisp.common.protocol.body.ClusterInfo;
import cn.xianyijun.wisp.common.protocol.body.ConsumeMessageDirectlyResult;
import cn.xianyijun.wisp.common.protocol.body.ConsumeStatsList;
import cn.xianyijun.wisp.common.protocol.body.ConsumerConnection;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.body.GetConsumerListByGroupResponseBody;
import cn.xianyijun.wisp.common.protocol.body.GetConsumerStatusBody;
import cn.xianyijun.wisp.common.protocol.body.GroupList;
import cn.xianyijun.wisp.common.protocol.body.KVTable;
import cn.xianyijun.wisp.common.protocol.body.LockBatchRequestBody;
import cn.xianyijun.wisp.common.protocol.body.LockBatchResponseBody;
import cn.xianyijun.wisp.common.protocol.body.ProducerConnection;
import cn.xianyijun.wisp.common.protocol.body.QueryConsumeQueueResponseBody;
import cn.xianyijun.wisp.common.protocol.body.QueryConsumeTimeSpanBody;
import cn.xianyijun.wisp.common.protocol.body.QueueTimeSpan;
import cn.xianyijun.wisp.common.protocol.body.ResetOffsetBody;
import cn.xianyijun.wisp.common.protocol.body.SubscriptionGroupWrapper;
import cn.xianyijun.wisp.common.protocol.body.TopicConfigSerializeWrapper;
import cn.xianyijun.wisp.common.protocol.body.TopicList;
import cn.xianyijun.wisp.common.protocol.body.UnlockBatchRequestBody;
import cn.xianyijun.wisp.common.protocol.header.CloneGroupOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ConsumeMessageDirectlyResultRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ConsumerSendMsgBackRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.CreateTopicRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.DeleteSubscriptionGroupRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.DeleteTopicRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.EndTransactionRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetConsumeStatsInBrokerHeader;
import cn.xianyijun.wisp.common.protocol.header.GetConsumeStatsRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetConsumerConnectionListRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetConsumerListByGroupRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetConsumerRunningInfoRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetConsumerStatusRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetEarliestMsgStoreTimeRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetEarliestMsgStoreTimeResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.GetMaxOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetMaxOffsetResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.GetMinOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetMinOffsetResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.GetProducerConnectionListRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.GetTopicStatsInfoRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.PullMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.PullMessageResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryConsumeQueueRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryConsumeTimeSpanRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryConsumerOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryConsumerOffsetResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryTopicConsumeByWhoRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ResetOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.SearchOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.SearchOffsetResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.UnregisterClientRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.UpdateConsumerOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ViewBrokerStatsDataRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ViewMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.filtersrv.RegisterMessageFilterClassRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.DeleteKVConfigRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetKVConfigRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetKVConfigResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetKVListByNamespaceRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetTopicsByClusterRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.PutKVConfigRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.WipeWritePermOfBrokerRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.WipeWritePermOfBrokerResponseHeader;
import cn.xianyijun.wisp.common.protocol.heartbeat.HeartbeatData;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.common.subscription.SubscriptionGroupConfig;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.InvokeCallback;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.remoting.RemotingClient;
import cn.xianyijun.wisp.remoting.netty.NettyClientConfig;
import cn.xianyijun.wisp.remoting.netty.NettyRemotingClient;
import cn.xianyijun.wisp.remoting.protocol.LanguageCode;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class MQClient {

    private final RemotingClient remotingClient;
    private final TopAddressing topAddressing;
    private final ClientRemotingProcessor clientRemotingProcessor;
    private String nameSrvAddr = null;
    private ClientConfig clientConfig;

    public MQClient(final NettyClientConfig nettyClientConfig,
                    final ClientRemotingProcessor clientRemotingProcessor,
                    RPCHook rpcHook, final ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        topAddressing = new TopAddressing(MixAll.getWSAddr(), clientConfig.getUnitName());
        this.remotingClient = new NettyRemotingClient(nettyClientConfig, null);
        this.clientRemotingProcessor = clientRemotingProcessor;

        this.remotingClient.registerRPCHook(rpcHook);

        this.remotingClient.registerProcessor(RequestCode.CHECK_TRANSACTION_STATE, this.clientRemotingProcessor, null);

        this.remotingClient.registerProcessor(RequestCode.NOTIFY_CONSUMER_IDS_CHANGED, this.clientRemotingProcessor, null);

        this.remotingClient.registerProcessor(RequestCode.RESET_CONSUMER_CLIENT_OFFSET, this.clientRemotingProcessor, null);

        this.remotingClient.registerProcessor(RequestCode.GET_CONSUMER_STATUS_FROM_CLIENT, this.clientRemotingProcessor, null);

        this.remotingClient.registerProcessor(RequestCode.GET_CONSUMER_RUNNING_INFO, this.clientRemotingProcessor, null);

        this.remotingClient.registerProcessor(RequestCode.CONSUME_MESSAGE_DIRECTLY, this.clientRemotingProcessor, null);
    }

    public void start() {
        this.remotingClient.start();
    }

    public void shutdown() {
        this.remotingClient.shutdown();
    }

    public void updateNameServerAddressList(final String addrListStr) {
        String[] addrArray = addrListStr.split(";");
        List<String> lst = new ArrayList<>(Arrays.asList(addrArray));

        this.remotingClient.updateNameServerAddressList(lst);
    }

    public int sendHearBeat(
            final String addr,
            final HeartbeatData heartbeatData,
            final long timeoutMillis
    ) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);

        request.setBody(heartbeatData.encode());
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return response.getVersion();
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public void registerMessageFilterClass(final String addr,
                                           final String consumerGroup,
                                           final String topic,
                                           final String className,
                                           final int classCRC,
                                           final byte[] classBody,
                                           final long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
            InterruptedException, BrokerException {
        RegisterMessageFilterClassRequestHeader requestHeader = new RegisterMessageFilterClassRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setClassName(className);
        requestHeader.setTopic(topic);
        requestHeader.setClassCRC(classCRC);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REGISTER_MESSAGE_FILTER_CLASS, requestHeader);
        request.setBody(classBody);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public String fetchNameServerAddr() {
        try {
            String nameServerAddr = this.topAddressing.fetchNameServerAddr();
            if (!StringUtils.isEmpty(nameServerAddr) && !nameServerAddr.equals(this.nameSrvAddr)) {
                log.info("name server address changed, old=" + this.nameSrvAddr + ", new=" + nameServerAddr);
                this.updateNameServerAddressList(nameServerAddr);
                this.nameSrvAddr = nameServerAddr;
                return nameSrvAddr;
            }
        } catch (Exception e) {
            log.error("fetchNameServerAddr Exception", e);
        }
        return nameSrvAddr;
    }

    public List<String> getNameServerAddressList() {
        return this.remotingClient.getNameServerAddressList();
    }

    public TopicRouteData getDefaultTopicRouteInfoFromNameServer(final String topic, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {

        return getTopicRouteInfoFromNameServer(topic, timeoutMillis, false);
    }

    public TopicRouteData getTopicRouteInfoFromNameServer(final String topic, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {

        return getTopicRouteInfoFromNameServer(topic, timeoutMillis, true);
    }

    public TopicRouteData getTopicRouteInfoFromNameServer(final String topic, final long timeoutMillis,
                                                          boolean allowTopicNotExist) throws ClientException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        GetRouteInfoRequestHeader requestHeader = new GetRouteInfoRequestHeader();
        requestHeader.setTopic(topic);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ROUTEINTO_BY_TOPIC, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        log.info("[MQClient] getTopicRouteInfoFromNameServer, topic: {} , request: {},  response: {}", topic, request, response);
        switch (response.getCode()) {
            case ResponseCode.TOPIC_NOT_EXIST: {
                if (allowTopicNotExist && !topic.equals(MixAll.DEFAULT_TOPIC)) {
                    log.warn("get Topic [{}] RouteInfoFromNameServer is not exist value", topic);
                }
                break;
            }
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return TopicRouteData.decode(body, TopicRouteData.class);
                }
            }
            default:
                break;
        }
        throw new ClientException(response.getCode(), response.getRemark());
    }

    public void unregisterClient(
            final String addr,
            final String clientID,
            final String producerGroup,
            final String consumerGroup,
            final long timeoutMillis
    ) throws RemotingException, BrokerException, InterruptedException {
        final UnregisterClientRequestHeader requestHeader = new UnregisterClientRequestHeader();
        requestHeader.setClientID(clientID);
        requestHeader.setProducerGroup(producerGroup);
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UNREGISTER_CLIENT, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public SendResult sendMessage(
            final String addr,
            final String brokerName,
            final Message msg,
            final ProduceMessageRequestHeader requestHeader,
            final long timeoutMillis,
            final CommunicationMode communicationMode,
            final SendMessageContext context,
            final ProducerDelegate producer
    ) throws RemotingException, BrokerException, InterruptedException {
        return sendMessage(addr, brokerName, msg, requestHeader, timeoutMillis, communicationMode, null, null, null, 0, context, producer);
    }

    public SendResult sendMessage(
            final String addr,
            final String brokerName,
            final Message msg,
            final ProduceMessageRequestHeader requestHeader,
            final long timeoutMillis,
            final CommunicationMode communicationMode,
            final SendCallback sendCallback,
            final TopicPublishInfo topicPublishInfo,
            final ClientFactory instance,
            final int retryTimesWhenSendFailed,
            final SendMessageContext context,
            final ProducerDelegate producer
    ) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = null;
        if (msg instanceof BatchMessage) {
            request = RemotingCommand.createRequestCommand(RequestCode.SEND_BATCH_MESSAGE, requestHeader);
        } else {
            request = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, requestHeader);
        }

        request.setBody(msg.getBody());

        switch (communicationMode) {
            case ONEWAY:
                this.remotingClient.invokeOneWay(addr, request, timeoutMillis);
                return null;
            case ASYNC:
                final AtomicInteger times = new AtomicInteger();
                this.sendMessageAsync(addr, brokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance,
                        retryTimesWhenSendFailed, times, context, producer);
                return null;
            case SYNC:
                return this.sendMessageSync(addr, brokerName, msg, timeoutMillis, request);
            default:
                break;
        }

        return null;
    }

    private SendResult sendMessageSync(
            final String addr,
            final String brokerName,
            final Message msg,
            final long timeoutMillis,
            final RemotingCommand request
    ) throws RemotingException, BrokerException, InterruptedException {
        log.info("[MQClient] sendMessageSync ,addr :{} ,brokerName: {} , msg:{} , request :{}", addr, brokerName, msg, request);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        return this.processSendResponse(brokerName, msg, response);
    }

    private void sendMessageAsync(
            final String addr,
            final String brokerName,
            final Message msg,
            final long timeoutMillis,
            final RemotingCommand request,
            final SendCallback sendCallback,
            final TopicPublishInfo topicPublishInfo,
            final ClientFactory instance,
            final int retryTimesWhenSendFailed,
            final AtomicInteger times,
            final SendMessageContext context,
            final ProducerDelegate producer
    ) throws InterruptedException, RemotingException {
        this.remotingClient.invokeAsync(addr, request, timeoutMillis, responseFuture -> {
            RemotingCommand response = responseFuture.getResponseCommand();
            if (null == sendCallback && response != null) {

                try {
                    SendResult sendResult = MQClient.this.processSendResponse(brokerName, msg, response);
                    if (context != null && sendResult != null) {
                        context.setSendResult(sendResult);
                        context.getProducer().executeSendMessageHookAfter(context);
                    }
                } catch (Throwable ignored) {
                }

                producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), false);
                return;
            }

            if (response != null) {
                try {
                    SendResult sendResult = MQClient.this.processSendResponse(brokerName, msg, response);
                    assert sendResult != null;
                    if (context != null) {
                        context.setSendResult(sendResult);
                        context.getProducer().executeSendMessageHookAfter(context);
                    }

                    try {
                        sendCallback.onSuccess(sendResult);
                    } catch (Throwable e) {
                    }

                    producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), false);
                } catch (Exception e) {
                    producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), true);
                    onDoException(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance,
                            retryTimesWhenSendFailed, times, e, context, false, producer);
                }
            } else {
                producer.updateFaultItem(brokerName, System.currentTimeMillis() - responseFuture.getBeginTimestamp(), true);
                if (!responseFuture.isSendRequestOK()) {
                    ClientException ex = new ClientException("send request failed", responseFuture.getCause());
                    onDoException(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance,
                            retryTimesWhenSendFailed, times, ex, context, true, producer);
                } else if (responseFuture.isTimeout()) {
                    ClientException ex = new ClientException("wait response timeout " + responseFuture.getTimeoutMillis() + "ms",
                            responseFuture.getCause());
                    onDoException(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance,
                            retryTimesWhenSendFailed, times, ex, context, true, producer);
                } else {
                    ClientException ex = new ClientException("unknow reseaon", responseFuture.getCause());
                    onDoException(brokerName, msg, 0L, request, sendCallback, topicPublishInfo, instance,
                            retryTimesWhenSendFailed, times, ex, context, true, producer);
                }
            }
        });
    }

    private void onDoException(String brokerName, Message msg, long timeoutMillis, RemotingCommand request, SendCallback sendCallback, TopicPublishInfo topicPublishInfo, ClientFactory instance, int timesTotal, AtomicInteger curTimes, Exception e, SendMessageContext context, boolean needRetry, ProducerDelegate producer) {
        int tmp = curTimes.incrementAndGet();
        if (needRetry && tmp <= timesTotal) {
            String retryBrokerName = brokerName;
            if (topicPublishInfo != null) {
                MessageQueue mqChosen = producer.selectOneMessageQueue(topicPublishInfo, brokerName);
                retryBrokerName = mqChosen.getBrokerName();
            }
            String addr = instance.findBrokerAddressInPublish(retryBrokerName);
            log.info("async send msg by retry {} times. topic={}, brokerAddr={}, brokerName={}", tmp, msg.getTopic(), addr,
                    retryBrokerName);
            try {
                request.setOpaque(RemotingCommand.createNewRequestId());
                sendMessageAsync(addr, retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance,
                        timesTotal, curTimes, context, producer);
            } catch (InterruptedException | RemotingTooMuchRequestException e1) {
                onDoException(retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, e1,
                        context, false, producer);
            } catch (RemotingException e1) {
                producer.updateFaultItem(brokerName, 3000, true);
                onDoException(retryBrokerName, msg, timeoutMillis, request, sendCallback, topicPublishInfo, instance, timesTotal, curTimes, e1,
                        context, true, producer);
            }
        } else {

            if (context != null) {
                context.setException(e);
                context.getProducer().executeSendMessageHookAfter(context);
            }

            try {
                sendCallback.onException(e);
            } catch (Exception ignored) {
            }
        }
    }

    private SendResult processSendResponse(
            final String brokerName,
            final Message msg,
            final RemotingCommand response
    ) throws BrokerException {
        log.info("[MQClient] processSendResponse, msg:{} ,response: {}", msg, response);
        switch (response.getCode()) {
            case ResponseCode.FLUSH_DISK_TIMEOUT:
            case ResponseCode.FLUSH_SLAVE_TIMEOUT:
            case ResponseCode.SLAVE_NOT_AVAILABLE: {
            }
            case ResponseCode.SUCCESS: {
                SendStatus sendStatus = SendStatus.SEND_OK;
                switch (response.getCode()) {
                    case ResponseCode.FLUSH_DISK_TIMEOUT:
                        sendStatus = SendStatus.FLUSH_DISK_TIMEOUT;
                        break;
                    case ResponseCode.FLUSH_SLAVE_TIMEOUT:
                        sendStatus = SendStatus.FLUSH_SLAVE_TIMEOUT;
                        break;
                    case ResponseCode.SLAVE_NOT_AVAILABLE:
                        sendStatus = SendStatus.SLAVE_NOT_AVAILABLE;
                        break;
                    case ResponseCode.SUCCESS:
                        sendStatus = SendStatus.SEND_OK;
                        break;
                    default:
                        break;
                }

                ProduceMessageResponseHeader responseHeader =
                        (ProduceMessageResponseHeader) response.decodeCommandCustomHeader(ProduceMessageResponseHeader.class);

                MessageQueue messageQueue = new MessageQueue(msg.getTopic(), brokerName, responseHeader.getQueueId());

                String uniqueMsgId = MessageClientIDSetter.getUniqueID(msg);
                if (msg instanceof BatchMessage) {
                    StringBuilder sb = new StringBuilder();
                    for (Message message : (BatchMessage) msg) {
                        sb.append(sb.length() == 0 ? "" : ",").append(MessageClientIDSetter.getUniqueID(message));
                    }
                    uniqueMsgId = sb.toString();
                }
                SendResult sendResult = new SendResult(sendStatus,
                        uniqueMsgId,
                        responseHeader.getMsgId(), messageQueue, responseHeader.getQueueOffset());
                sendResult.setTransactionId(responseHeader.getTransactionId());
                String regionId = response.getExtFields().get(MessageConst.PROPERTY_MSG_REGION);
                String traceOn = response.getExtFields().get(MessageConst.PROPERTY_TRACE_SWITCH);
                if (regionId == null || regionId.isEmpty()) {
                    regionId = MixAll.DEFAULT_TRACE_REGION_ID;
                }
                if (traceOn != null && "false".equals(traceOn)) {
                    sendResult.setTraceOn(false);
                } else {
                    sendResult.setTraceOn(true);
                }
                sendResult.setRegionId(regionId);
                return sendResult;
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public void checkClientInBroker(final String brokerAddr, final String consumerGroup,
                                    final String clientId, final SubscriptionData subscriptionData,
                                    final long timeoutMillis)
            throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
            RemotingConnectException, ClientException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CHECK_CLIENT_CONFIG, null);

        CheckClientRequestBody requestBody = new CheckClientRequestBody();
        requestBody.setClientId(clientId);
        requestBody.setGroup(consumerGroup);
        requestBody.setSubscriptionData(subscriptionData);

        request.setBody(requestBody.encode());

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), request, timeoutMillis);

        if (ResponseCode.SUCCESS != response.getCode()) {
            throw new ClientException(response.getCode(), response.getRemark());
        }
    }

    public void createTopic(final String addr, final String defaultTopic, final TopicConfig topicConfig,
                            final long timeoutMillis)
            throws RemotingException, InterruptedException, ClientException {
        CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
        requestHeader.setTopic(topicConfig.getTopicName());
        requestHeader.setDefaultTopic(defaultTopic);
        requestHeader.setReadQueueNums(topicConfig.getReadQueueNums());
        requestHeader.setWriteQueueNums(topicConfig.getWriteQueueNums());
        requestHeader.setPerm(topicConfig.getPerm());
        requestHeader.setTopicFilterType(topicConfig.getTopicFilterType().name());
        requestHeader.setTopicSysFlag(topicConfig.getTopicSysFlag());
        requestHeader.setOrder(topicConfig.isOrder());

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public long searchOffset(final String addr, final String topic, final int queueId, final long timestamp,
                             final long timeoutMillis)
            throws RemotingException, BrokerException, InterruptedException {
        SearchOffsetRequestHeader requestHeader = new SearchOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(queueId);
        requestHeader.setTimestamp(timestamp);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.SEARCH_OFFSET_BY_TIMESTAMP, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                SearchOffsetResponseHeader responseHeader =
                        (SearchOffsetResponseHeader) response.decodeCommandCustomHeader(SearchOffsetResponseHeader.class);
                return responseHeader.getOffset();
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public long getMinOffset(final String addr, final String topic, final int queueId, final long timeoutMillis)
            throws RemotingException, BrokerException, InterruptedException {
        GetMinOffsetRequestHeader requestHeader = new GetMinOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(queueId);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_MIN_OFFSET, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                GetMinOffsetResponseHeader responseHeader =
                        (GetMinOffsetResponseHeader) response.decodeCommandCustomHeader(GetMinOffsetResponseHeader.class);

                return responseHeader.getOffset();
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public long getMaxOffset(final String addr, final String topic, final int queueId, final long timeoutMillis)
            throws RemotingException, BrokerException, InterruptedException {
        GetMaxOffsetRequestHeader requestHeader = new GetMaxOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(queueId);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_MAX_OFFSET, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                GetMaxOffsetResponseHeader responseHeader =
                        (GetMaxOffsetResponseHeader) response.decodeCommandCustomHeader(GetMaxOffsetResponseHeader.class);

                return responseHeader.getOffset();
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public long getEarliestMsgStoretime(final String addr, final String topic, final int queueId,
                                        final long timeoutMillis)
            throws RemotingException, BrokerException, InterruptedException {
        GetEarliestMsgStoreTimeRequestHeader requestHeader = new GetEarliestMsgStoreTimeRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(queueId);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_EARLIEST_MSG_STORETIME, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                GetEarliestMsgStoreTimeResponseHeader responseHeader =
                        (GetEarliestMsgStoreTimeResponseHeader) response.decodeCommandCustomHeader(GetEarliestMsgStoreTimeResponseHeader.class);

                return responseHeader.getTimestamp();
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public ExtMessage viewMessage(final String addr, final long phyOffset, final long timeoutMillis)
            throws RemotingException, BrokerException, InterruptedException {
        ViewMessageRequestHeader requestHeader = new ViewMessageRequestHeader();
        requestHeader.setOffset(phyOffset);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.VIEW_MESSAGE_BY_ID, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                ByteBuffer byteBuffer = ByteBuffer.wrap(response.getBody());
                return MessageDecoder.clientDecode(byteBuffer, true);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public List<String> getConsumerIdListByGroup(
            final String addr,
            final String consumerGroup,
            final long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
            BrokerException, InterruptedException {
        GetConsumerListByGroupRequestHeader requestHeader = new GetConsumerListByGroupRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_LIST_BY_GROUP, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                if (response.getBody() != null) {
                    GetConsumerListByGroupResponseBody body =
                            GetConsumerListByGroupResponseBody.decode(response.getBody(), GetConsumerListByGroupResponseBody.class);
                    return body.getConsumerIdList();
                }
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public Set<MessageQueue> lockBatchMQ(
            final String addr,
            final LockBatchRequestBody requestBody,
            final long timeoutMillis) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.LOCK_BATCH_MQ, null);

        request.setBody(requestBody.encode());
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                LockBatchResponseBody responseBody = LockBatchResponseBody.decode(response.getBody(), LockBatchResponseBody.class);
                Set<MessageQueue> messageQueues = responseBody.getLockOKMQSet();
                return messageQueues;
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public PullResult pullMessage(
            final String addr,
            final PullMessageRequestHeader requestHeader,
            final long timeoutMillis,
            final CommunicationMode communicationMode,
            final PullCallback pullCallback
    ) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.PULL_MESSAGE, requestHeader);

        switch (communicationMode) {
            case ONEWAY:
                assert false;
                return null;
            case ASYNC:
                this.pullMessageAsync(addr, request, timeoutMillis, pullCallback);
                return null;
            case SYNC:
                return this.pullMessageSync(addr, request, timeoutMillis);
            default:
                assert false;
                break;
        }

        return null;
    }

    private void pullMessageAsync(
            final String addr,
            final RemotingCommand request,
            final long timeoutMillis,
            final PullCallback pullCallback
    ) throws RemotingException, InterruptedException {
        this.remotingClient.invokeAsync(addr, request, timeoutMillis, responseFuture -> {
            RemotingCommand response = responseFuture.getResponseCommand();
            if (response != null) {
                try {
                    PullResult pullResult = MQClient.this.processPullResponse(response);
                    pullCallback.onSuccess(pullResult);
                } catch (Exception e) {
                    pullCallback.onException(e);
                }
            } else {
                if (!responseFuture.isSendRequestOK()) {
                    pullCallback.onException(new ClientException("send request failed to " + addr + ". Request: " + request, responseFuture.getCause()));
                } else if (responseFuture.isTimeout()) {
                    pullCallback.onException(new ClientException("wait response from " + addr + " timeout :" + responseFuture.getTimeoutMillis() + "ms" + ". Request: " + request,
                            responseFuture.getCause()));
                } else {
                    pullCallback.onException(new ClientException("unknown reason. addr: " + addr + ", timeoutMillis: " + timeoutMillis + ". Request: " + request, responseFuture.getCause()));
                }
            }
        });
    }

    private PullResult pullMessageSync(
            final String addr,
            final RemotingCommand request,
            final long timeoutMillis
    ) throws RemotingException, InterruptedException, BrokerException {
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        assert response != null;
        return this.processPullResponse(response);
    }

    private PullResult processPullResponse(
            final RemotingCommand response) throws BrokerException {
        PullStatus pullStatus;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS:
                pullStatus = PullStatus.FOUND;
                break;
            case ResponseCode.PULL_NOT_FOUND:
                pullStatus = PullStatus.NO_NEW_MSG;
                break;
            case ResponseCode.PULL_RETRY_IMMEDIATELY:
                pullStatus = PullStatus.NO_MATCHED_MSG;
                break;
            case ResponseCode.PULL_OFFSET_MOVED:
                pullStatus = PullStatus.OFFSET_ILLEGAL;
                break;

            default:
                throw new BrokerException(response.getCode(), response.getRemark());
        }

        PullMessageResponseHeader responseHeader =
                (PullMessageResponseHeader) response.decodeCommandCustomHeader(PullMessageResponseHeader.class);

        return new ExtPullResult(pullStatus, responseHeader.getNextBeginOffset(), responseHeader.getMinOffset(),
                responseHeader.getMaxOffset(), null, responseHeader.getSuggestWhichBrokerId(), response.getBody());
    }

    public void unlockBatchMQ(
            final String addr,
            final UnlockBatchRequestBody requestBody,
            final long timeoutMillis,
            final boolean oneWay
    ) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UNLOCK_BATCH_MQ, null);

        request.setBody(requestBody.encode());

        if (oneWay) {
            this.remotingClient.invokeOneWay(addr, request, timeoutMillis);
        } else {
            RemotingCommand response = this.remotingClient
                    .invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
            switch (response.getCode()) {
                case ResponseCode.SUCCESS: {
                    return;
                }
                default:
                    break;
            }

            throw new BrokerException(response.getCode(), response.getRemark());
        }
    }

    public void consumerSendMessageBack(
            final String addr,
            final ExtMessage msg,
            final String consumerGroup,
            final int delayLevel,
            final long timeoutMillis,
            final int maxConsumeRetryTimes
    ) throws RemotingException, BrokerException, InterruptedException {
        ConsumerSendMsgBackRequestHeader requestHeader = new ConsumerSendMsgBackRequestHeader();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CONSUMER_SEND_MSG_BACK, requestHeader);

        requestHeader.setGroup(consumerGroup);
        requestHeader.setOriginTopic(msg.getTopic());
        requestHeader.setOffset(msg.getCommitLogOffset());
        requestHeader.setDelayLevel(delayLevel);
        requestHeader.setOriginMsgId(msg.getMsgId());
        requestHeader.setMaxReConsumeTimes(maxConsumeRetryTimes);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public long queryConsumerOffset(
            final String addr,
            final QueryConsumerOffsetRequestHeader requestHeader,
            final long timeoutMillis
    ) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUMER_OFFSET, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                QueryConsumerOffsetResponseHeader responseHeader =
                        (QueryConsumerOffsetResponseHeader) response.decodeCommandCustomHeader(QueryConsumerOffsetResponseHeader.class);

                return responseHeader.getOffset();
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public void updateConsumerOffsetOneWay(
            final String addr,
            final UpdateConsumerOffsetRequestHeader requestHeader,
            final long timeoutMillis
    ) throws RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException,
            InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_CONSUMER_OFFSET, requestHeader);

        this.remotingClient.invokeOneWay(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
    }

    public void updateConsumerOffset(
            final String addr,
            final UpdateConsumerOffsetRequestHeader requestHeader,
            final long timeoutMillis
    ) throws RemotingException, BrokerException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_CONSUMER_OFFSET, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }


    public void queryMessage(
            final String addr,
            final QueryMessageRequestHeader requestHeader,
            final long timeoutMillis,
            final InvokeCallback invokeCallback,
            final Boolean isUniqueKey
    ) throws RemotingException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.QUERY_MESSAGE, requestHeader);
        request.addExtField(MixAll.UNIQUE_MSG_QUERY_FLAG, isUniqueKey.toString());
        this.remotingClient.invokeAsync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis,
                invokeCallback);
    }

    public void endTransactionOneWay(
            final String addr,
            final EndTransactionRequestHeader requestHeader,
            final String remark,
            final long timeoutMillis
    ) throws RemotingException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.END_TRANSACTION, requestHeader);

        request.setRemark(remark);
        this.remotingClient.invokeOneWay(addr, request, timeoutMillis);
    }

    public Properties getBrokerConfig(final String addr, final long timeoutMillis)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException,
            BrokerException, UnsupportedEncodingException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CONFIG, null);

        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return MixAll.string2Properties(new String(response.getBody(), MixAll.DEFAULT_CHARSET));
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public void updateBrokerConfig(final String addr, final Properties properties, final long timeoutMillis)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException,
            BrokerException, UnsupportedEncodingException {

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_BROKER_CONFIG, null);

        String str = MixAll.properties2String(properties);
        if (str.length() > 0) {
            request.setBody(str.getBytes(MixAll.DEFAULT_CHARSET));
            RemotingCommand response = this.remotingClient
                    .invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr), request, timeoutMillis);
            switch (response.getCode()) {
                case ResponseCode.SUCCESS: {
                    return;
                }
                default:
                    break;
            }

            throw new BrokerException(response.getCode(), response.getRemark());
        }
    }

    public void createSubscriptionGroup(final String addr, final SubscriptionGroupConfig config,
                                        final long timeoutMillis)
            throws RemotingException, InterruptedException, ClientException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_SUBSCRIPTIONGROUP, null);

        byte[] body = RemotingSerializable.encode(config);
        request.setBody(body);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public TopicStatsTable getTopicStatsInfo(final String addr, final String topic,
                                             final long timeoutMillis) throws InterruptedException,
            RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, BrokerException {
        GetTopicStatsInfoRequestHeader requestHeader = new GetTopicStatsInfoRequestHeader();
        requestHeader.setTopic(topic);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_TOPIC_STATS_INFO, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return TopicStatsTable.decode(response.getBody(), TopicStatsTable.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public TopicList getTopicListFromNameServer(final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_LIST_FROM_NAMESERVER, null);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return TopicList.decode(body, TopicList.class);
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public TopicList getTopicsByCluster(final String cluster, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {
        GetTopicsByClusterRequestHeader requestHeader = new GetTopicsByClusterRequestHeader();
        requestHeader.setCluster(cluster);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_TOPICS_BY_CLUSTER, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return TopicList.decode(body, TopicList.class);
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public KVTable getBrokerRuntimeInfo(final String addr, final long timeoutMillis) throws RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException {

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_RUNTIME_INFO, null);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return KVTable.decode(response.getBody(), KVTable.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public ConsumeStats getConsumeStats(final String addr, final String consumerGroup, final long timeoutMillis)
            throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException,
            BrokerException {
        return getConsumeStats(addr, consumerGroup, null, timeoutMillis);
    }

    public ConsumeStats getConsumeStats(final String addr, final String consumerGroup, final String topic,
                                        final long timeoutMillis)
            throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException,
            BrokerException {
        GetConsumeStatsRequestHeader requestHeader = new GetConsumeStatsRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setTopic(topic);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUME_STATS, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ConsumeStats.decode(response.getBody(), ConsumeStats.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public ClusterInfo getBrokerClusterInfo(
            final long timeoutMillis) throws InterruptedException, RemotingTimeoutException,
            RemotingSendRequestException, RemotingConnectException, BrokerException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CLUSTER_INFO, null);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ClusterInfo.decode(response.getBody(), ClusterInfo.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public ProducerConnection getProducerConnectionList(final String addr, final String producerGroup,
                                                        final long timeoutMillis)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException,
            BrokerException {
        GetProducerConnectionListRequestHeader requestHeader = new GetProducerConnectionListRequestHeader();
        requestHeader.setProducerGroup(producerGroup);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_PRODUCER_CONNECTION_LIST, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ProducerConnection.decode(response.getBody(), ProducerConnection.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public ConsumerConnection getConsumerConnectionList(final String addr, final String consumerGroup,
                                                        final long timeoutMillis)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException,
            BrokerException {
        GetConsumerConnectionListRequestHeader requestHeader = new GetConsumerConnectionListRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_CONNECTION_LIST, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ConsumerConnection.decode(response.getBody(), ConsumerConnection.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public int wipeWritePermOfBroker(final String namesrvAddr, String brokerName,
                                     final long timeoutMillis) throws
            RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, ClientException {
        WipeWritePermOfBrokerRequestHeader requestHeader = new WipeWritePermOfBrokerRequestHeader();
        requestHeader.setBrokerName(brokerName);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.WIPE_WRITE_PERM_OF_BROKER, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(namesrvAddr, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                WipeWritePermOfBrokerResponseHeader responseHeader =
                        (WipeWritePermOfBrokerResponseHeader) response.decodeCommandCustomHeader(WipeWritePermOfBrokerResponseHeader.class);
                return responseHeader.getWipeTopicCount();
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public String getKVConfigValue(final String namespace, final String key, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {
        GetKVConfigRequestHeader requestHeader = new GetKVConfigRequestHeader();
        requestHeader.setNamespace(namespace);
        requestHeader.setKey(key);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_KV_CONFIG, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                GetKVConfigResponseHeader responseHeader =
                        (GetKVConfigResponseHeader) response.decodeCommandCustomHeader(GetKVConfigResponseHeader.class);
                return responseHeader.getValue();
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public KVTable getKVListByNamespace(final String namespace, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {
        GetKVListByNamespaceRequestHeader requestHeader = new GetKVListByNamespaceRequestHeader();
        requestHeader.setNamespace(namespace);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_KVLIST_BY_NAMESPACE, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(null, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return KVTable.decode(response.getBody(), KVTable.class);
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public void deleteTopicInBroker(final String addr, final String topic, final long timeoutMillis)
            throws RemotingException, InterruptedException, ClientException {
        DeleteTopicRequestHeader requestHeader = new DeleteTopicRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_BROKER, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public void deleteTopicInNameServer(final String addr, final String topic, final long timeoutMillis)
            throws RemotingException, InterruptedException, ClientException {
        DeleteTopicRequestHeader requestHeader = new DeleteTopicRequestHeader();
        requestHeader.setTopic(topic);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_NAMESRV, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public void deleteSubscriptionGroup(final String addr, final String groupName, final long timeoutMillis)
            throws RemotingException, InterruptedException, ClientException {
        DeleteSubscriptionGroupRequestHeader requestHeader = new DeleteSubscriptionGroupRequestHeader();
        requestHeader.setGroupName(groupName);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_SUBSCRIPTIONGROUP, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public void putKVConfigValue(final String namespace, final String key, final String value, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {
        PutKVConfigRequestHeader requestHeader = new PutKVConfigRequestHeader();
        requestHeader.setNamespace(namespace);
        requestHeader.setKey(key);
        requestHeader.setValue(value);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.PUT_KV_CONFIG, requestHeader);

        List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();
        if (nameServerAddressList != null) {
            RemotingCommand errResponse = null;
            for (String namesrvAddr : nameServerAddressList) {
                RemotingCommand response = this.remotingClient.invokeSync(namesrvAddr, request, timeoutMillis);
                switch (response.getCode()) {
                    case ResponseCode.SUCCESS: {
                        break;
                    }
                    default:
                        errResponse = response;
                }
            }

            if (errResponse != null) {
                throw new ClientException(errResponse.getCode(), errResponse.getRemark());
            }
        }
    }

    public void deleteKVConfigValue(final String namespace, final String key, final long timeoutMillis)
            throws RemotingException, ClientException, InterruptedException {
        DeleteKVConfigRequestHeader requestHeader = new DeleteKVConfigRequestHeader();
        requestHeader.setNamespace(namespace);
        requestHeader.setKey(key);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_KV_CONFIG, requestHeader);

        List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();
        if (nameServerAddressList != null) {
            RemotingCommand errResponse = null;
            for (String namesrvAddr : nameServerAddressList) {
                RemotingCommand response = this.remotingClient.invokeSync(namesrvAddr, request, timeoutMillis);
                assert response != null;
                switch (response.getCode()) {
                    case ResponseCode.SUCCESS: {
                        break;
                    }
                    default:
                        errResponse = response;
                }
            }
            if (errResponse != null) {
                throw new ClientException(errResponse.getCode(), errResponse.getRemark());
            }
        }
    }

    public Map<MessageQueue, Long> invokeBrokerToResetOffset(final String addr, final String topic, final String group,
                                                             final long timestamp, final boolean isForce, final long timeoutMillis, boolean isC)
            throws RemotingException, ClientException, InterruptedException {
        ResetOffsetRequestHeader requestHeader = new ResetOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        requestHeader.setTimestamp(timestamp);
        requestHeader.setForce(isForce);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.INVOKE_BROKER_TO_RESET_OFFSET, requestHeader);
        if (isC) {
            request.setLanguage(LanguageCode.CPP);
        }

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                if (response.getBody() != null) {
                    ResetOffsetBody body = ResetOffsetBody.decode(response.getBody(), ResetOffsetBody.class);
                    return body.getOffsetTable();
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public Map<String, Map<MessageQueue, Long>> invokeBrokerToGetConsumerStatus(final String addr, final String topic,
                                                                                final String group,
                                                                                final String clientAddr,
                                                                                final long timeoutMillis) throws RemotingException, ClientException, InterruptedException {
        GetConsumerStatusRequestHeader requestHeader = new GetConsumerStatusRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        requestHeader.setClientAddr(clientAddr);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.INVOKE_BROKER_TO_GET_CONSUMER_STATUS, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                if (response.getBody() != null) {
                    GetConsumerStatusBody body = GetConsumerStatusBody.decode(response.getBody(), GetConsumerStatusBody.class);
                    return body.getConsumerTable();
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }


    public GroupList queryTopicConsumeByWho(final String addr, final String topic, final long timeoutMillis)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException,
            BrokerException {
        QueryTopicConsumeByWhoRequestHeader requestHeader = new QueryTopicConsumeByWhoRequestHeader();
        requestHeader.setTopic(topic);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return GroupList.decode(response.getBody(), GroupList.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public List<QueueTimeSpan> queryConsumeTimeSpan(final String addr, final String topic, final String group,
                                                    final long timeoutMillis)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException,
            BrokerException {
        QueryConsumeTimeSpanRequestHeader requestHeader = new QueryConsumeTimeSpanRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUME_TIME_SPAN, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                QueryConsumeTimeSpanBody consumeTimeSpanBody = GroupList.decode(response.getBody(), QueryConsumeTimeSpanBody.class);
                return consumeTimeSpanBody.getConsumeTimeSpanSet();
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public boolean cleanExpiredConsumeQueue(final String addr,
                                            long timeoutMillis) throws ClientException, RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CLEAN_EXPIRED_CONSUMEQUEUE, null);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return true;
            }
            default:
                break;
        }
        throw new ClientException(response.getCode(), response.getRemark());
    }

    public boolean cleanUnusedTopicByAddr(final String addr,
                                          long timeoutMillis) throws ClientException, RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CLEAN_UNUSED_TOPIC, null);
        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return true;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public ConsumerRunningInfo getConsumerRunningInfo(final String addr, String consumerGroup, String clientId,
                                                      boolean jstack,
                                                      final long timeoutMillis) throws RemotingException, ClientException, InterruptedException {
        GetConsumerRunningInfoRequestHeader requestHeader = new GetConsumerRunningInfoRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setClientId(clientId);
        requestHeader.setJstackEnable(jstack);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_RUNNING_INFO, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    ConsumerRunningInfo info = ConsumerRunningInfo.decode(body, ConsumerRunningInfo.class);
                    return info;
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public ConsumeMessageDirectlyResult consumeMessageDirectly(final String addr,
                                                               String consumerGroup,
                                                               String clientId,
                                                               String msgId,
                                                               final long timeoutMillis) throws RemotingException, ClientException, InterruptedException {
        ConsumeMessageDirectlyResultRequestHeader requestHeader = new ConsumeMessageDirectlyResultRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setClientId(clientId);
        requestHeader.setMsgId(msgId);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CONSUME_MESSAGE_DIRECTLY, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    ConsumeMessageDirectlyResult info = ConsumeMessageDirectlyResult.decode(body, ConsumeMessageDirectlyResult.class);
                    return info;
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public void cloneGroupOffset(final String addr, final String srcGroup, final String destGroup, final String topic,
                                 final boolean isOffline,
                                 final long timeoutMillis) throws RemotingException, ClientException, InterruptedException {
        CloneGroupOffsetRequestHeader requestHeader = new CloneGroupOffsetRequestHeader();
        requestHeader.setSrcGroup(srcGroup);
        requestHeader.setDestGroup(destGroup);
        requestHeader.setTopic(topic);
        requestHeader.setOffline(isOffline);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CLONE_GROUP_OFFSET, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }


    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey, long timeoutMillis)
            throws ClientException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
            InterruptedException {
        ViewBrokerStatsDataRequestHeader requestHeader = new ViewBrokerStatsDataRequestHeader();
        requestHeader.setStatsName(statsName);
        requestHeader.setStatsKey(statsKey);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.VIEW_BROKER_STATS_DATA, requestHeader);

        RemotingCommand response = this.remotingClient
                .invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return BrokerStatsData.decode(body, BrokerStatsData.class);
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public Set<String> getClusterList(String topic,
                                      long timeoutMillis) {
        return Collections.emptySet();
    }

    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder,
                                                      long timeoutMillis) throws ClientException,
            RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        GetConsumeStatsInBrokerHeader requestHeader = new GetConsumeStatsInBrokerHeader();
        requestHeader.setOrder(isOrder);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CONSUME_STATS, requestHeader);

        RemotingCommand response = this.remotingClient
                .invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return ConsumeStatsList.decode(body, ConsumeStatsList.class);
                }
            }
            default:
                break;
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

    public SubscriptionGroupWrapper getAllSubscriptionGroup(final String brokerAddr,
                                                            long timeoutMillis) throws InterruptedException,
            RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, BrokerException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = this.remotingClient
                .invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return SubscriptionGroupWrapper.decode(response.getBody(), SubscriptionGroupWrapper.class);
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public TopicConfigSerializeWrapper getAllTopicConfig(final String addr,
                                                         long timeoutMillis) throws RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
                request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return TopicConfigSerializeWrapper.decode(response.getBody(), TopicConfigSerializeWrapper.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public void updateNameServerConfig(final Properties properties, final List<String> nameServers, long timeoutMillis)
            throws UnsupportedEncodingException,
            BrokerException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
            RemotingConnectException, ClientException {
        String str = MixAll.properties2String(properties);
        if (str == null || str.length() < 1) {
            return;
        }
        List<String> invokeNameServers = (nameServers == null || nameServers.isEmpty()) ?
                this.remotingClient.getNameServerAddressList() : nameServers;
        if (invokeNameServers == null || invokeNameServers.isEmpty()) {
            return;
        }

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_NAMESRV_CONFIG, null);
        request.setBody(str.getBytes(MixAll.DEFAULT_CHARSET));

        RemotingCommand errResponse = null;
        for (String nameServer : invokeNameServers) {
            RemotingCommand response = this.remotingClient.invokeSync(nameServer, request, timeoutMillis);
            switch (response.getCode()) {
                case ResponseCode.SUCCESS: {
                    break;
                }
                default:
                    errResponse = response;
            }
        }

        if (errResponse != null) {
            throw new ClientException(errResponse.getCode(), errResponse.getRemark());
        }
    }

    public Map<String, Properties> getNameServerConfig(final List<String> nameServers, long timeoutMillis)
            throws InterruptedException,
            RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException,
            ClientException, UnsupportedEncodingException {
        List<String> invokeNameServers = (nameServers == null || nameServers.isEmpty()) ?
                this.remotingClient.getNameServerAddressList() : nameServers;
        if (invokeNameServers == null || invokeNameServers.isEmpty()) {
            return null;
        }

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_NAMESRV_CONFIG, null);

        Map<String, Properties> configMap = new HashMap<String, Properties>(4);
        for (String nameServer : invokeNameServers) {
            RemotingCommand response = this.remotingClient.invokeSync(nameServer, request, timeoutMillis);

            assert response != null;

            if (ResponseCode.SUCCESS == response.getCode()) {
                configMap.put(nameServer, MixAll.string2Properties(new String(response.getBody(), MixAll.DEFAULT_CHARSET)));
            } else {
                throw new ClientException(response.getCode(), response.getRemark());
            }
        }
        return configMap;
    }

    public QueryConsumeQueueResponseBody queryConsumeQueue(final String brokerAddr, final String topic,
                                                           final int queueId,
                                                           final long index, final int count, final String consumerGroup,
                                                           final long timeoutMillis) throws InterruptedException,
            RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException {

        QueryConsumeQueueRequestHeader requestHeader = new QueryConsumeQueueRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(queueId);
        requestHeader.setIndex(index);
        requestHeader.setCount(count);
        requestHeader.setConsumerGroup(consumerGroup);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUME_QUEUE, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), brokerAddr), request, timeoutMillis);

        assert response != null;

        if (ResponseCode.SUCCESS == response.getCode()) {
            return QueryConsumeQueueResponseBody.decode(response.getBody(), QueryConsumeQueueResponseBody.class);
        }

        throw new ClientException(response.getCode(), response.getRemark());
    }

}
