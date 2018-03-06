package cn.xianyijun.wisp.broker.client.net;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.client.ClientChannelInfo;
import cn.xianyijun.wisp.broker.client.ConsumerGroupInfo;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.body.GetConsumerStatusBody;
import cn.xianyijun.wisp.common.protocol.body.ResetOffsetBody;
import cn.xianyijun.wisp.common.protocol.header.GetConsumerStatusRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.NotifyConsumerIdsChangedRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.ResetOffsetRequestHeader;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.utils.StringUtils;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class BrokerClient {
    private final BrokerController brokerController;

    public RemotingCommand resetOffset(String topic, String group, long timeStamp, boolean isForce) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        TopicConfig topicConfig = this.brokerController.getTopicConfigManager().selectTopicConfig(topic);
        if (null == topicConfig) {
            log.error("[reset-offset] reset offset failed, no topic in this broker. topic={}", topic);
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark("[reset-offset] reset offset failed, no topic in this broker. topic=" + topic);
            return response;
        }

        Map<MessageQueue, Long> offsetTable = new HashMap<>();

        for (int i = 0; i < topicConfig.getWriteQueueNums(); i++) {
            MessageQueue mq = new MessageQueue();
            mq.setBrokerName(this.brokerController.getBrokerConfig().getBrokerName());
            mq.setTopic(topic);
            mq.setQueueId(i);

            long consumerOffset =
                    this.brokerController.getConsumerOffsetManager().queryOffset(group, topic, i);
            if (-1 == consumerOffset) {
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark(String.format("THe consumer group <%s> not exist", group));
                return response;
            }

            long timeStampOffset;
            if (timeStamp == -1) {
                timeStampOffset = this.brokerController.getMessageStore().getMaxOffsetInQueue(topic, i);
            } else {
                timeStampOffset = this.brokerController.getMessageStore().getOffsetInQueueByTime(topic, i, timeStamp);
            }

            if (timeStampOffset < 0) {
                log.warn("reset offset is invalid. topic={}, queueId={}, timeStampOffset={}", topic, i, timeStampOffset);
                timeStampOffset = 0;
            }

            if (isForce || timeStampOffset < consumerOffset) {
                offsetTable.put(mq, timeStampOffset);
            } else {
                offsetTable.put(mq, consumerOffset);
            }
        }

        ResetOffsetRequestHeader requestHeader = new ResetOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        requestHeader.setTimestamp(timeStamp);
        RemotingCommand request =
                RemotingCommand.createRequestCommand(RequestCode.RESET_CONSUMER_CLIENT_OFFSET, requestHeader);
        ResetOffsetBody body = new ResetOffsetBody();
        body.setOffsetTable(offsetTable);
        request.setBody(body.encode());

        ConsumerGroupInfo consumerGroupInfo =
                this.brokerController.getConsumerManager().getConsumerGroupInfo(group);

        if (consumerGroupInfo != null && !consumerGroupInfo.getAllChannel().isEmpty()) {
            ConcurrentMap<Channel, ClientChannelInfo> channelInfoTable =
                    consumerGroupInfo.getChannelInfoTable();
            for (Map.Entry<Channel, ClientChannelInfo> entry : channelInfoTable.entrySet()) {
                try {
                    this.brokerController.getRemotingServer().invokeOneWay(entry.getKey(), request, 5000);
                    log.info("[reset-offset] reset offset success. topic={}, group={}, clientId={}",
                            topic, group, entry.getValue().getClientId());
                } catch (Exception e) {
                    log.error("[reset-offset] reset offset exception. topic={}, group={}",
                            new Object[] {topic, group}, e);
                }
            }
        } else {
            String errorInfo =
                    String.format("Consumer not online, so can not reset offset, Group: %s Topic: %s Timestamp: %d",
                            requestHeader.getGroup(),
                            requestHeader.getTopic(),
                            requestHeader.getTimestamp());
            log.error(errorInfo);
            response.setCode(ResponseCode.CONSUMER_NOT_ONLINE);
            response.setRemark(errorInfo);
            return response;
        }
        response.setCode(ResponseCode.SUCCESS);
        ResetOffsetBody resBody = new ResetOffsetBody();
        resBody.setOffsetTable(offsetTable);
        response.setBody(resBody.encode());
        return response;
    }

    public RemotingCommand getConsumeStatus(String topic, String group, String originClientId) {
        final RemotingCommand result = RemotingCommand.createResponseCommand(null);

        GetConsumerStatusRequestHeader requestHeader = new GetConsumerStatusRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setGroup(group);
        RemotingCommand request =
                RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_STATUS_FROM_CLIENT,
                        requestHeader);

        Map<String, Map<MessageQueue, Long>> consumerStatusTable =
                new HashMap<>();
        ConcurrentMap<Channel, ClientChannelInfo> channelInfoTable =
                this.brokerController.getConsumerManager().getConsumerGroupInfo(group).getChannelInfoTable();
        if (null == channelInfoTable || channelInfoTable.isEmpty()) {
            result.setCode(ResponseCode.SYSTEM_ERROR);
            result.setRemark(String.format("No Any Consumer online in the consumer group: [%s]", group));
            return result;
        }

        for (Map.Entry<Channel, ClientChannelInfo> entry : channelInfoTable.entrySet()) {
            String clientId = entry.getValue().getClientId();
            if (StringUtils.isEmpty(originClientId) || originClientId.equals(clientId)) {
                try {
                    RemotingCommand response =
                            this.brokerController.getRemotingServer().invokeSync(entry.getKey(), request, 5000);
                    switch (response.getCode()) {
                        case ResponseCode.SUCCESS: {
                            if (response.getBody() != null) {
                                GetConsumerStatusBody body =
                                        GetConsumerStatusBody.decode(response.getBody(),
                                                GetConsumerStatusBody.class);

                                consumerStatusTable.put(clientId, body.getMessageQueueTable());
                                log.info(
                                        "[get-consumer-status] get consumer status success. topic={}, group={}, channelRemoteAddr={}",
                                        topic, group, clientId);
                            }
                        }
                        default:
                            break;
                    }
                } catch (Exception e) {
                    log.error(
                            "[get-consumer-status] get consumer status exception. topic={}, group={}, offset={}",
                            new Object[] {topic, group}, e);
                }

                if (!StringUtils.isEmpty(originClientId) && originClientId.equals(clientId)) {
                    break;
                }
            }
        }

        result.setCode(ResponseCode.SUCCESS);
        GetConsumerStatusBody resBody = new GetConsumerStatusBody();
        resBody.setConsumerTable(consumerStatusTable);
        result.setBody(resBody.encode());
        return result;
    }


    public RemotingCommand callClient(final Channel channel,
                                      final RemotingCommand request
    ) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        return this.brokerController.getRemotingServer().invokeSync(channel, request, 10000);
    }

    public void notifyConsumerIdsChanged(
            final Channel channel,
            final String consumerGroup) {
        if (null == consumerGroup) {
            log.error("notifyConsumerIdsChanged consumerGroup is null");
            return;
        }

        NotifyConsumerIdsChangedRequestHeader requestHeader = new NotifyConsumerIdsChangedRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);
        RemotingCommand request =
                RemotingCommand.createRequestCommand(RequestCode.NOTIFY_CONSUMER_IDS_CHANGED, requestHeader);

        try {
            this.brokerController.getRemotingServer().invokeOneWay(channel, request, 10);
        } catch (Exception e) {
            log.error("notifyConsumerIdsChanged exception, " + consumerGroup, e.getMessage());
        }
    }


}
