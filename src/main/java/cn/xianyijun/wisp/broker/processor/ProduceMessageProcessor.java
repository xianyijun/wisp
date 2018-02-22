package cn.xianyijun.wisp.broker.processor;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.mqtrace.ConsumeMessageHook;
import cn.xianyijun.wisp.broker.mqtrace.ProduceMessageContext;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.constant.PermName;
import cn.xianyijun.wisp.common.message.ExtBatchMessage;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageResponseHeader;
import cn.xianyijun.wisp.common.subscription.SubscriptionGroupConfig;
import cn.xianyijun.wisp.common.sysflag.MessageSysFlag;
import cn.xianyijun.wisp.exception.RemotingCommandException;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.store.MessageExtBrokerInner;
import cn.xianyijun.wisp.store.PutMessageResult;
import cn.xianyijun.wisp.store.config.StorePathConfigHelper;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static cn.xianyijun.wisp.common.protocol.ResponseCode.FLUSH_DISK_TIMEOUT;
import static cn.xianyijun.wisp.common.protocol.ResponseCode.FLUSH_SLAVE_TIMEOUT;
import static cn.xianyijun.wisp.common.protocol.ResponseCode.MESSAGE_ILLEGAL;
import static cn.xianyijun.wisp.common.protocol.ResponseCode.SERVICE_NOT_AVAILABLE;
import static cn.xianyijun.wisp.common.protocol.ResponseCode.SLAVE_NOT_AVAILABLE;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ProduceMessageProcessor extends AbstractProduceMessageProcessor implements NettyRequestProcessor {
    @Setter
    private List<ConsumeMessageHook> consumeMessageHookList;

    public ProduceMessageProcessor(BrokerController brokerController) {
        super(brokerController);
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        ProduceMessageContext mqTraceContext;

        switch (request.getCode()) {
            case RequestCode.CONSUMER_SEND_MSG_BACK:
                return this.consumerSendMsgBack(ctx, request);
            default:
                ProduceMessageRequestHeader requestHeader = parseRequestHeader(request);
                if (requestHeader == null) {
                    return null;
                }

                mqTraceContext = buildMsgContext(ctx, requestHeader);
                this.executeSendMessageHookBefore(ctx, request, mqTraceContext);

                RemotingCommand response;
                if (requestHeader.isBatch()) {
                    response = this.produceBatchMessage(ctx, request, mqTraceContext, requestHeader);
                } else {
                    response = this.produceMessage(ctx, request, mqTraceContext, requestHeader);
                }

                this.executeSendMessageHookAfter(response, mqTraceContext);
                return response;
        }
    }

    private RemotingCommand produceMessage(final ChannelHandlerContext ctx,
                                           final RemotingCommand request,
                                           final ProduceMessageContext produceMessageContext,
                                           final ProduceMessageRequestHeader requestHeader) throws RemotingCommandException {

        final RemotingCommand response = RemotingCommand.createResponseCommand(ProduceMessageResponseHeader.class);
        final ProduceMessageResponseHeader responseHeader = (ProduceMessageResponseHeader) response.getCustomHeader();

        response.setOpaque(request.getOpaque());

        response.addExtField(MessageConst.PROPERTY_MSG_REGION, this.brokerController.getBrokerConfig().getRegionId());
        response.addExtField(MessageConst.PROPERTY_TRACE_SWITCH, String.valueOf(this.brokerController.getBrokerConfig().isTraceOn()));

        log.debug("receive SendMessage request command, {}", request);

        final long startTimstamp = this.brokerController.getBrokerConfig().getStartAcceptSendRequestTimeStamp();
        if (this.brokerController.getMessageStore().now() < startTimstamp) {
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark(String.format("broker unable to service, until %s", UtilAll.timeMillisToHumanString(startTimstamp)));
            return response;
        }

        response.setCode(-1);
        super.msgCheck(ctx, requestHeader, response);
        if (response.getCode() != -1) {
            return response;
        }

        final byte[] body = request.getBody();

        int queueIdInt = requestHeader.getQueueId();
        TopicConfig topicConfig = this.brokerController.getTopicConfigManager().selectTopicConfig(requestHeader.getTopic());

        if (queueIdInt < 0) {
            queueIdInt = Math.abs(this.random.nextInt() % 99999999) % topicConfig.getWriteQueueNums();
        }

        MessageExtBrokerInner msgInner = new MessageExtBrokerInner();
        msgInner.setTopic(requestHeader.getTopic());
        msgInner.setQueueId(queueIdInt);

        if (!handleRetryAndDLQ(requestHeader, response, request, msgInner, topicConfig)) {
            return response;
        }

        msgInner.setBody(body);
        msgInner.setFlag(requestHeader.getFlag());
        MessageAccessor.setProperties(msgInner, MessageDecoder.string2messageProperties(requestHeader.getProperties()));
        msgInner.setPropertiesString(requestHeader.getProperties());
        msgInner.setBornTimestamp(requestHeader.getBornTimestamp());
        msgInner.setBornHost(ctx.channel().remoteAddress());
        msgInner.setStoreHost(this.getStoreHost());
        msgInner.setReConsumeTimes(requestHeader.getReConsumeTimes() == null ? 0 : requestHeader.getReConsumeTimes());

        if (this.brokerController.getBrokerConfig().isRejectTransactionMessage()) {
            String traFlag = msgInner.getProperty(MessageConst.PROPERTY_TRANSACTION_PREPARED);
            if (traFlag != null) {
                response.setCode(ResponseCode.NO_PERMISSION);
                response.setRemark(
                        "the broker[" + this.brokerController.getBrokerConfig().getBrokerIP1() + "] sending transaction message is forbidden");
                return response;
            }
        }

        PutMessageResult putMessageResult = this.brokerController.getMessageStore().putMessage(msgInner);

        return handlePutMessageResult(putMessageResult, response, request, msgInner, responseHeader, produceMessageContext, ctx, queueIdInt);

    }

    private RemotingCommand produceBatchMessage(ChannelHandlerContext ctx, RemotingCommand request, ProduceMessageContext produceMessageContext, ProduceMessageRequestHeader requestHeader) {

        final RemotingCommand response = RemotingCommand.createResponseCommand(ProduceMessageResponseHeader.class);
        final ProduceMessageResponseHeader responseHeader = (ProduceMessageResponseHeader) response.getCustomHeader();

        response.setOpaque(request.getOpaque());

        response.addExtField(MessageConst.PROPERTY_MSG_REGION, this.brokerController.getBrokerConfig().getRegionId());
        response.addExtField(MessageConst.PROPERTY_TRACE_SWITCH, String.valueOf(this.brokerController.getBrokerConfig().isTraceOn()));

        final long startTimestamp = this.brokerController.getBrokerConfig().getStartAcceptSendRequestTimeStamp();
        if (this.brokerController.getMessageStore().now() < startTimestamp) {
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark(String.format("broker unable to service, until %s", UtilAll.timeMillisToHumanString(startTimestamp)));
            return response;
        }

        response.setCode(-1);
        super.msgCheck(ctx, requestHeader, response);
        if (response.getCode() != -1) {
            return response;
        }

        int queueIdInt = requestHeader.getQueueId();
        TopicConfig topicConfig = this.brokerController.getTopicConfigManager().selectTopicConfig(requestHeader.getTopic());

        if (queueIdInt < 0) {
            queueIdInt = Math.abs(this.random.nextInt() % 99999999) % topicConfig.getWriteQueueNums();
        }

        if (requestHeader.getTopic().length() > Byte.MAX_VALUE) {
            response.setCode(MESSAGE_ILLEGAL);
            response.setRemark("message topic length too long " + requestHeader.getTopic().length());
            return response;
        }

        if (requestHeader.getTopic() != null && requestHeader.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            response.setCode(MESSAGE_ILLEGAL);
            response.setRemark("batch request does not support retry group " + requestHeader.getTopic());
            return response;
        }
        ExtBatchMessage messageExtBatch = new ExtBatchMessage();
        messageExtBatch.setTopic(requestHeader.getTopic());
        messageExtBatch.setQueueId(queueIdInt);

        int sysFlag = requestHeader.getSysFlag();
        if (TopicFilterType.MULTI_TAG == topicConfig.getTopicFilterType()) {
            sysFlag |= MessageSysFlag.MULTI_TAGS_FLAG;
        }
        messageExtBatch.setSysFlag(sysFlag);

        messageExtBatch.setFlag(requestHeader.getFlag());
        MessageAccessor.setProperties(messageExtBatch, MessageDecoder.string2messageProperties(requestHeader.getProperties()));
        messageExtBatch.setBody(request.getBody());
        messageExtBatch.setBornTimestamp(requestHeader.getBornTimestamp());
        messageExtBatch.setBornHost(ctx.channel().remoteAddress());
        messageExtBatch.setStoreHost(this.getStoreHost());
        messageExtBatch.setReConsumeTimes(requestHeader.getReConsumeTimes() == null ? 0 : requestHeader.getReConsumeTimes());

        PutMessageResult putMessageResult = this.brokerController.getMessageStore().putMessages(messageExtBatch);

        return handlePutMessageResult(putMessageResult, response, request, messageExtBatch, responseHeader, produceMessageContext, ctx, queueIdInt);
    }

    private RemotingCommand handlePutMessageResult(PutMessageResult putMessageResult, RemotingCommand response,
                                                   RemotingCommand request, ExtMessage msg,
                                                   ProduceMessageResponseHeader responseHeader, ProduceMessageContext produceMessageContext, ChannelHandlerContext ctx,
                                                   int queueIdInt) {
        if (putMessageResult == null) {
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark("store putMessage return null");
            return response;
        }
        boolean sendOK = false;

        switch (putMessageResult.getPutMessageStatus()) {
            // Success
            case PUT_OK:
                sendOK = true;
                response.setCode(ResponseCode.SUCCESS);
                break;
            case FLUSH_DISK_TIMEOUT:
                response.setCode(FLUSH_DISK_TIMEOUT);
                sendOK = true;
                break;
            case FLUSH_SLAVE_TIMEOUT:
                response.setCode(FLUSH_SLAVE_TIMEOUT);
                sendOK = true;
                break;
            case SLAVE_NOT_AVAILABLE:
                response.setCode(SLAVE_NOT_AVAILABLE);
                sendOK = true;
                break;

            // Failed
            case CREATE_MAPEDDFILE_FAILED:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("create mapped file failed, server is busy or broken.");
                break;
            case MESSAGE_ILLEGAL:
            case PROPERTIES_SIZE_EXCEEDED:
                response.setCode(MESSAGE_ILLEGAL);
                response.setRemark(
                        "the message is illegal, maybe msg body or properties length not matched. msg body length limit 128k, msg properties length limit 32k.");
                break;
            case SERVICE_NOT_AVAILABLE:
                response.setCode(SERVICE_NOT_AVAILABLE);
                response.setRemark(
                        "service not available now, maybe disk full, " + diskUtil() + ", maybe your broker machine memory too small.");
                break;
            case OS_PAGECACHE_BUSY:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("[PC_SYNCHRONIZED]broker busy, start flow control for a while");
                break;
            case UNKNOWN_ERROR:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("UNKNOWN_ERROR");
                break;
            default:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("UNKNOWN_ERROR DEFAULT");
                break;
        }

        String owner = request.getExtFields().get(BrokerStatsManager.COMMERCIAL_OWNER);
        if (sendOK) {

            this.brokerController.getBrokerStatsManager().incTopicPutNums(msg.getTopic(), putMessageResult.getAppendMessageResult().getMsgNum(), 1);
            this.brokerController.getBrokerStatsManager().incTopicPutSize(msg.getTopic(),
                    putMessageResult.getAppendMessageResult().getWroteBytes());
            this.brokerController.getBrokerStatsManager().incBrokerPutNums(putMessageResult.getAppendMessageResult().getMsgNum());

            response.setRemark(null);

            responseHeader.setMsgId(putMessageResult.getAppendMessageResult().getMsgId());
            responseHeader.setQueueId(queueIdInt);
            responseHeader.setQueueOffset(putMessageResult.getAppendMessageResult().getLogicOffset());

            doResponse(ctx, request, response);

            if (hasProduceMessageHook()) {
                produceMessageContext.setMsgId(responseHeader.getMsgId());
                produceMessageContext.setQueueId(responseHeader.getQueueId());
                produceMessageContext.setQueueOffset(responseHeader.getQueueOffset());

                int commercialBaseCount = brokerController.getBrokerConfig().getCommercialBaseCount();
                int wroteSize = putMessageResult.getAppendMessageResult().getWroteBytes();
                int incValue = (int) Math.ceil(wroteSize / BrokerStatsManager.SIZE_PER_COUNT) * commercialBaseCount;

                produceMessageContext.setCommercialSendStats(BrokerStatsManager.StatsType.SEND_SUCCESS);
                produceMessageContext.setCommercialSendTimes(incValue);
                produceMessageContext.setCommercialSendSize(wroteSize);
                produceMessageContext.setCommercialOwner(owner);
            }
            return null;
        } else {
            if (hasProduceMessageHook()) {
                int wroteSize = request.getBody().length;
                int incValue = (int) Math.ceil(wroteSize / BrokerStatsManager.SIZE_PER_COUNT);

                produceMessageContext.setCommercialSendStats(BrokerStatsManager.StatsType.SEND_FAILURE);
                produceMessageContext.setCommercialSendTimes(incValue);
                produceMessageContext.setCommercialSendSize(wroteSize);
                produceMessageContext.setCommercialOwner(owner);
            }
        }
        return response;
    }


    protected void doResponse(ChannelHandlerContext ctx, RemotingCommand request,
                              final RemotingCommand response) {
        if (!request.isOneWayRPC()) {
            try {
                ctx.writeAndFlush(response);
            } catch (Throwable e) {
                log.error("SendMessageProcessor process request over, but response failed", e);
                log.error(request.toString());
                log.error(response.toString());
            }
        }
    }

    private boolean handleRetryAndDLQ(ProduceMessageRequestHeader requestHeader, RemotingCommand response,
                                      RemotingCommand request,
                                      ExtMessage msg, TopicConfig topicConfig) {
        String newTopic = requestHeader.getTopic();
        if (null != newTopic && newTopic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            String groupName = newTopic.substring(MixAll.RETRY_GROUP_TOPIC_PREFIX.length());
            SubscriptionGroupConfig subscriptionGroupConfig =
                    this.brokerController.getSubscriptionGroupManager().findSubscriptionGroupConfig(groupName);
            if (null == subscriptionGroupConfig) {
                response.setCode(ResponseCode.SUBSCRIPTION_GROUP_NOT_EXIST);
                response.setRemark(
                        "subscription group not exist, " + groupName);
                return false;
            }

            int maxReConsumeTimes = requestHeader.getMaxReConsumeTimes();
            int reConsumeTimes = requestHeader.getReConsumeTimes() == null ? 0 : requestHeader.getReConsumeTimes();
            if (reConsumeTimes >= maxReConsumeTimes) {
                newTopic = MixAll.getDLQTopic(groupName);
                int queueIdInt = Math.abs(this.random.nextInt() % 99999999) % DLQ_NUMS_PER_GROUP;
                topicConfig = this.brokerController.getTopicConfigManager().createTopicInProduceMessageBackMethod(newTopic,
                        DLQ_NUMS_PER_GROUP,
                        PermName.PERM_WRITE, 0
                );
                msg.setTopic(newTopic);
                msg.setQueueId(queueIdInt);
                if (null == topicConfig) {
                    response.setCode(ResponseCode.SYSTEM_ERROR);
                    response.setRemark("topic[" + newTopic + "] not exist");
                    return false;
                }
            }
        }
        int sysFlag = requestHeader.getSysFlag();
        if (TopicFilterType.MULTI_TAG == topicConfig.getTopicFilterType()) {
            sysFlag |= MessageSysFlag.MULTI_TAGS_FLAG;
        }
        msg.setSysFlag(sysFlag);
        return true;
    }

    private ProduceMessageContext buildMsgContext(ChannelHandlerContext ctx, ProduceMessageRequestHeader requestHeader) {
        if (!this.hasProduceMessageHook()) {
            return null;
        }
        ProduceMessageContext mqTraceContext;
        mqTraceContext = new ProduceMessageContext();
        mqTraceContext.setProducerGroup(requestHeader.getProducerGroup());
        mqTraceContext.setTopic(requestHeader.getTopic());
        mqTraceContext.setMsgProps(requestHeader.getProperties());
        mqTraceContext.setBornHost(RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
        mqTraceContext.setBrokerAddr(this.brokerController.getBrokerAddr());
        mqTraceContext.setBrokerRegionId(this.brokerController.getBrokerConfig().getRegionId());
        mqTraceContext.setBornTimeStamp(requestHeader.getBornTimestamp());

        Map<String, String> properties = MessageDecoder.string2messageProperties(requestHeader.getProperties());
        String uniqueKey = properties.get(MessageConst.PROPERTY_UNIQUE_CLIENT_MESSAGE_ID_KEYIDX);
        properties.put(MessageConst.PROPERTY_MSG_REGION, this.brokerController.getBrokerConfig().getRegionId());
        properties.put(MessageConst.PROPERTY_TRACE_SWITCH, String.valueOf(this.brokerController.getBrokerConfig().isTraceOn()));
        requestHeader.setProperties(MessageDecoder.messageProperties2String(properties));

        if (uniqueKey == null) {
            uniqueKey = "";
        }
        mqTraceContext.setMsgUniqueKey(uniqueKey);
        return mqTraceContext;
    }


    private String diskUtil() {
        String storePathPhysic = this.brokerController.getMessageStoreConfig().getStorePathCommitLog();
        double physicRatio = UtilAll.getDiskPartitionSpaceUsedPercent(storePathPhysic);

        String storePathLogic =
                StorePathConfigHelper.getStorePathConsumeQueue(this.brokerController.getMessageStoreConfig().getStorePathRootDir());
        double logicRatio = UtilAll.getDiskPartitionSpaceUsedPercent(storePathLogic);

        String storePathIndex =
                StorePathConfigHelper.getStorePathIndex(this.brokerController.getMessageStoreConfig().getStorePathRootDir());
        double indexRatio = UtilAll.getDiskPartitionSpaceUsedPercent(storePathIndex);

        return String.format("CL: %5.2f CQ: %5.2f INDEX: %5.2f", physicRatio, logicRatio, indexRatio);
    }

    private RemotingCommand consumerSendMsgBack(ChannelHandlerContext ctx, RemotingCommand request) {
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
