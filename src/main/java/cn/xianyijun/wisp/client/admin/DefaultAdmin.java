package cn.xianyijun.wisp.client.admin;

import cn.xianyijun.wisp.client.producer.TopicPublishInfo;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageId;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.route.BrokerData;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class DefaultAdmin {

    private final ClientInstance clientFactory;
    private long timeoutMillis = 6000;


    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws ClientException {
        try {
            TopicRouteData topicRouteData = this.clientFactory.getClient().getTopicRouteInfoFromNameServer(topic, timeoutMillis);
            if (topicRouteData != null) {
                TopicPublishInfo topicPublishInfo = ClientInstance.topicRouteData2TopicPublishInfo(topic, topicRouteData);
                if (topicPublishInfo != null && topicPublishInfo.ok()) {
                    return topicPublishInfo.getMessageQueueList();
                }
            }
        } catch (Exception e) {
            throw new ClientException("Can not find Message Queue for this topic, " + topic, e);
        }

        throw new ClientException("Unknow why, Can not find Message Queue for this topic, " + topic, null);
    }

    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        try {
            TopicRouteData topicRouteData = this.clientFactory.getClient().getTopicRouteInfoFromNameServer(key, timeoutMillis);
            List<BrokerData> brokerDataList = topicRouteData.getBrokerDatas();
            if (brokerDataList != null && !brokerDataList.isEmpty()) {
                Collections.sort(brokerDataList);

                boolean createOKAtLeastOnce = false;
                ClientException exception = null;

                StringBuilder orderTopicString = new StringBuilder();

                for (BrokerData brokerData : brokerDataList) {
                    String addr = brokerData.getBrokerAddrs().get(MixAll.MASTER_ID);
                    if (addr != null) {
                        TopicConfig topicConfig = new TopicConfig(newTopic);
                        topicConfig.setReadQueueNums(queueNum);
                        topicConfig.setWriteQueueNums(queueNum);
                        topicConfig.setTopicSysFlag(topicSysFlag);

                        boolean createOK = false;
                        for (int i = 0; i < 5; i++) {
                            try {
                                this.clientFactory.getClient().createTopic(addr, key, topicConfig, timeoutMillis);
                                createOK = true;
                                createOKAtLeastOnce = true;
                                break;
                            } catch (Exception e) {
                                if (4 == i) {
                                    exception = new ClientException("create topic to broker exception", e);
                                }
                            }
                        }

                        if (createOK) {
                            orderTopicString.append(brokerData.getBrokerName());
                            orderTopicString.append(":");
                            orderTopicString.append(queueNum);
                            orderTopicString.append(";");
                        }
                    }
                }

                if (exception != null && !createOKAtLeastOnce) {
                    throw exception;
                }
            } else {
                throw new ClientException("Not found broker, maybe key is wrong", null);
            }
        } catch (Exception e) {
            throw new ClientException("create new topic failed", e);
        }
    }

    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        String brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.clientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }

        if (brokerAddr != null) {
            try {
                return this.clientFactory.getClient().searchOffset(brokerAddr, mq.getTopic(), mq.getQueueId(), timestamp,
                        timeoutMillis);
            } catch (Exception e) {
                throw new ClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        }

        throw new ClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }

    public long maxOffset(MessageQueue mq) throws ClientException {
        String brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.clientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }

        if (brokerAddr != null) {
            try {
                return this.clientFactory.getClient().getMaxOffset(brokerAddr, mq.getTopic(), mq.getQueueId(), timeoutMillis);
            } catch (Exception e) {
                throw new ClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        }

        throw new ClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }

    public long minOffset(MessageQueue mq) throws ClientException {
        String brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.clientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }

        if (brokerAddr != null) {
            try {
                return this.clientFactory.getClient().getMinOffset(brokerAddr, mq.getTopic(), mq.getQueueId(), timeoutMillis);
            } catch (Exception e) {
                throw new ClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        }

        throw new ClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }

    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        String brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        if (null == brokerAddr) {
            this.clientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            brokerAddr = this.clientFactory.findBrokerAddressInPublish(mq.getBrokerName());
        }

        if (brokerAddr != null) {
            try {
                return this.clientFactory.getClient().getEarliestMsgStoretime(brokerAddr, mq.getTopic(), mq.getQueueId(),
                        timeoutMillis);
            } catch (Exception e) {
                throw new ClientException("Invoke Broker[" + brokerAddr + "] exception", e);
            }
        }

        throw new ClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }

    public ExtMessage viewMessage(
            String msgId) throws RemotingException, BrokerException, InterruptedException, ClientException {

        MessageId messageId;
        try {
            messageId = MessageDecoder.decodeMessageId(msgId);
        } catch (Exception e) {
            throw new ClientException(ResponseCode.NO_MESSAGE, "query message by id finished, but no message.");
        }
        return this.clientFactory.getClient().viewMessage(RemotingUtils.socketAddress2String(messageId.getAddress()),
                messageId.getOffset(), timeoutMillis);
    }

}
