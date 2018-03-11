package cn.xianyijun.wisp.broker.processor;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.mqtrace.ProduceMessageContext;
import cn.xianyijun.wisp.broker.mqtrace.ProduceMessageHook;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.constant.PermName;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ProduceMessageResponseHeader;
import cn.xianyijun.wisp.common.sysflag.TopicSysFlag;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.utils.CollectionUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Random;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public abstract class AbstractProduceMessageProcessor implements NettyRequestProcessor {

    protected final static int DLQ_NUMS_PER_GROUP = 1;
    protected final BrokerController brokerController;
    protected final Random random = new Random(System.currentTimeMillis());
    protected final SocketAddress storeHost;
    @Setter
    private List<ProduceMessageHook> produceMessageHookList;

    AbstractProduceMessageProcessor(final BrokerController brokerController) {
        this.brokerController = brokerController;
        this.storeHost =
                new InetSocketAddress(brokerController.getBrokerConfig().getBrokerIP1(), brokerController
                        .getNettyServerConfig().getListenPort());
    }


    ProduceMessageRequestHeader parseRequestHeader(RemotingCommand request) {
        log.info("[AbstractProduceMessageProcessor] parseRequestHeader , request: {}", request);
        ProduceMessageRequestHeader requestHeader = null;
        switch (request.getCode()) {
            case RequestCode.SEND_BATCH_MESSAGE:
            case RequestCode.SEND_MESSAGE:
                requestHeader = (ProduceMessageRequestHeader) request
                        .decodeCommandCustomHeader(ProduceMessageRequestHeader.class);
            default:
                break;
        }
        log.info("[AbstractProduceMessageProcessor] parseRequestHeader , requestHeader: {}", requestHeader);
        return requestHeader;
    }

    void msgCheck(final ChannelHandlerContext ctx,
                  final ProduceMessageRequestHeader requestHeader, final RemotingCommand response) {
        log.info("[AbstractProduceMessageProcessor] msgCheck , requestHeader:{} , response:{} ", requestHeader, response);

        if (!PermName.isWriteable(this.brokerController.getBrokerConfig().getBrokerPermission())
                && this.brokerController.getTopicConfigManager().isOrderTopic(requestHeader.getTopic())) {
            response.setCode(ResponseCode.NO_PERMISSION);
            response.setRemark("the broker[" + this.brokerController.getBrokerConfig().getBrokerIP1()
                    + "] sending message is forbidden");
            return;
        }

        if (!this.brokerController.getTopicConfigManager().isTopicCanProduceMessage(requestHeader.getTopic())) {
            String errorMsg = "the topic[" + requestHeader.getTopic() + "] is conflict with system reserved words.";
            log.warn(errorMsg);
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark(errorMsg);
            return;
        }

        TopicConfig topicConfig =
                this.brokerController.getTopicConfigManager().selectTopicConfig(requestHeader.getTopic());

        if (null == topicConfig) {
            int topicSysFlag = 0;
            if (requestHeader.isUnitMode()) {
                if (requestHeader.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    topicSysFlag = TopicSysFlag.buildSysFlag(false, true);
                } else {
                    topicSysFlag = TopicSysFlag.buildSysFlag(true, false);
                }
            }

            log.warn("the topic {} not exist, producer: {}", requestHeader.getTopic(), ctx.channel().remoteAddress());
            topicConfig = this.brokerController.getTopicConfigManager().createTopicInProduceMessageMethod(
                    requestHeader.getTopic(),
                    requestHeader.getDefaultTopic(),
                    RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
                    requestHeader.getDefaultTopicQueueNums(), topicSysFlag);
            log.info("[AbstractProduceProcessor] msgCheck , createTopic, topicConfig: {}", topicConfig);
            if (null == topicConfig) {
                if (requestHeader.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                    topicConfig =
                            this.brokerController.getTopicConfigManager().createTopicInProduceMessageBackMethod(
                                    requestHeader.getTopic(), 1, PermName.PERM_WRITE | PermName.PERM_READ,
                                    topicSysFlag);
                }
            }

            if (null == topicConfig) {
                response.setCode(ResponseCode.TOPIC_NOT_EXIST);
                response.setRemark("topic[" + requestHeader.getTopic() + "] not exist, apply first please!");
                return;
            }
        }

        int queueIdInt = requestHeader.getQueueId();
        int idValid = Math.max(topicConfig.getWriteQueueNums(), topicConfig.getReadQueueNums());
        if (queueIdInt >= idValid) {
            String errorInfo = String.format("request queueId[%d] is illegal, %s Producer: %s",
                    queueIdInt,
                    topicConfig.toString(),
                    RemotingHelper.parseChannelRemoteAddr(ctx.channel()));

            log.warn(errorInfo);
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark(errorInfo);
        }
    }

    boolean hasProduceMessageHook() {
        return CollectionUtils.isEmpty(produceMessageHookList);
    }

    void executeSendMessageHookBefore(ChannelHandlerContext ctx, RemotingCommand request, ProduceMessageContext context) {
        if (!hasProduceMessageHook()) {
            return;
        }
        for (ProduceMessageHook hook : this.produceMessageHookList) {
            try {
                final ProduceMessageRequestHeader requestHeader = parseRequestHeader(request);

                if (null != requestHeader) {
                    context.setProducerGroup(requestHeader.getProducerGroup());
                    context.setTopic(requestHeader.getTopic());
                    context.setBodyLength(request.getBody().length);
                    context.setMsgProps(requestHeader.getProperties());
                    context.setBornHost(RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                    context.setBrokerAddr(this.brokerController.getBrokerAddr());
                    context.setQueueId(requestHeader.getQueueId());
                }

                hook.sendMessageBefore(context);
                if (requestHeader != null) {
                    requestHeader.setProperties(context.getMsgProps());
                }
            } catch (Throwable e) {
                // Ignore
            }
        }
    }

    void executeSendMessageHookAfter(final RemotingCommand response, final ProduceMessageContext context) {
        if (!hasProduceMessageHook()) {
            return;
        }
        for (ProduceMessageHook hook : this.produceMessageHookList) {
            try {
                if (response != null) {
                    final ProduceMessageResponseHeader responseHeader =
                            (ProduceMessageResponseHeader) response.getCustomHeader();
                    context.setMsgId(responseHeader.getMsgId());
                    context.setQueueId(responseHeader.getQueueId());
                    context.setQueueOffset(responseHeader.getQueueOffset());
                    context.setCode(response.getCode());
                    context.setErrorMsg(response.getRemark());
                }
                hook.sendMessageAfter(context);
            } catch (Throwable ignored) {
            }
        }
    }
}
