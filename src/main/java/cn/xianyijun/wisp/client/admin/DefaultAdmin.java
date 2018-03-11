package cn.xianyijun.wisp.client.admin;

import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.producer.TopicPublishInfo;
import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageClientIDSetter;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageId;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.QueryMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryMessageResponseHeader;
import cn.xianyijun.wisp.common.protocol.route.BrokerData;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class DefaultAdmin {

    private final ClientFactory clientFactory;
    private long timeoutMillis = 6000;


    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws ClientException {
        try {
            TopicRouteData topicRouteData = this.clientFactory.getClient().getTopicRouteInfoFromNameServer(topic, timeoutMillis);
            if (topicRouteData != null) {
                TopicPublishInfo topicPublishInfo = ClientFactory.topicRouteData2TopicPublishInfo(topic, topicRouteData);
                if (topicPublishInfo != null && topicPublishInfo.ok()) {
                    return topicPublishInfo.getMessageQueueList();
                }
            }
        } catch (Exception e) {
            throw new ClientException("Can not find Message Queue for this topic, " + topic, e);
        }

        throw new ClientException("Unknown why, Can not find Message Queue for this topic, " + topic, null);
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

    public QueryResult queryMessage(String topic, String key, int maxNum, long begin,
                                    long end) throws ClientException,
            InterruptedException {
        return queryMessage(topic, key, maxNum, begin, end, false);
    }

    protected QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end,
                                       boolean isUniqueKey) throws ClientException,
            InterruptedException {
        TopicRouteData topicRouteData = this.clientFactory.getAnExistTopicRouteData(topic);
        if (null == topicRouteData) {
            this.clientFactory.updateTopicRouteInfoFromNameServer(topic);
            topicRouteData = this.clientFactory.getAnExistTopicRouteData(topic);
        }

        if (topicRouteData != null) {
            List<String> brokerAddrs = new LinkedList<String>();
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                String addr = brokerData.selectBrokerAddr();
                if (addr != null) {
                    brokerAddrs.add(addr);
                }
            }

            if (!brokerAddrs.isEmpty()) {
                final CountDownLatch countDownLatch = new CountDownLatch(brokerAddrs.size());
                final List<QueryResult> queryResultList = new LinkedList<QueryResult>();
                final ReadWriteLock lock = new ReentrantReadWriteLock(false);

                for (String addr : brokerAddrs) {
                    try {
                        QueryMessageRequestHeader requestHeader = new QueryMessageRequestHeader();
                        requestHeader.setTopic(topic);
                        requestHeader.setKey(key);
                        requestHeader.setMaxNum(maxNum);
                        requestHeader.setBeginTimestamp(begin);
                        requestHeader.setEndTimestamp(end);

                        this.clientFactory.getClient().queryMessage(addr, requestHeader, timeoutMillis * 3,
                                responseFuture -> {
                                    try {
                                        RemotingCommand response = responseFuture.getResponseCommand();
                                        if (response != null) {
                                            switch (response.getCode()) {
                                                case ResponseCode.SUCCESS: {
                                                    QueryMessageResponseHeader responseHeader = null;
                                                    responseHeader =
                                                            (QueryMessageResponseHeader) response
                                                                    .decodeCommandCustomHeader(QueryMessageResponseHeader.class);

                                                    List<ExtMessage> wrappers =
                                                            MessageDecoder.decodes(ByteBuffer.wrap(response.getBody()), true);

                                                    QueryResult qr = new QueryResult(responseHeader.getIndexLastUpdateTimestamp(), wrappers);
                                                    try {
                                                        lock.writeLock().lock();
                                                        queryResultList.add(qr);
                                                    } finally {
                                                        lock.writeLock().unlock();
                                                    }
                                                    break;
                                                }
                                                default:
                                                    log.warn("getResponseCommand failed, {} {}", response.getCode(), response.getRemark());
                                                    break;
                                            }
                                        } else {
                                            log.warn("getResponseCommand return null");
                                        }
                                    } finally {
                                        countDownLatch.countDown();
                                    }
                                }, isUniqueKey);
                    } catch (Exception e) {
                        log.warn("queryMessage exception", e);
                    }

                }

                boolean ok = countDownLatch.await(timeoutMillis * 4, TimeUnit.MILLISECONDS);
                if (!ok) {
                    log.warn("queryMessage, maybe some broker failed");
                }

                long indexLastUpdateTimestamp = 0;
                List<ExtMessage> messageList = new LinkedList<>();
                for (QueryResult qr : queryResultList) {
                    if (qr.getIndexLastUpdateTimestamp() > indexLastUpdateTimestamp) {
                        indexLastUpdateTimestamp = qr.getIndexLastUpdateTimestamp();
                    }

                    for (ExtMessage msgExt : qr.getMessageList()) {
                        if (isUniqueKey) {
                            if (msgExt.getMsgId().equals(key)) {

                                if (messageList.size() > 0) {

                                    if (messageList.get(0).getStoreTimestamp() > msgExt.getStoreTimestamp()) {

                                        messageList.clear();
                                        messageList.add(msgExt);
                                    }

                                } else {

                                    messageList.add(msgExt);
                                }
                            } else {
                                log.warn("queryMessage by uniqKey, find message key not matched, maybe hash duplicate {}", msgExt.toString());
                            }
                        } else {
                            String keys = msgExt.getKeys();
                            if (keys != null) {
                                boolean matched = false;
                                String[] keyArray = keys.split(MessageConst.KEY_SEPARATOR);
                                for (String k : keyArray) {
                                    if (key.equals(k)) {
                                        matched = true;
                                        break;
                                    }
                                }

                                if (matched) {
                                    messageList.add(msgExt);
                                } else {
                                    log.warn("queryMessage, find message key not matched, maybe hash duplicate {}", msgExt.toString());
                                }
                            }
                        }
                    }
                }

                if (!messageList.isEmpty()) {
                    return new QueryResult(indexLastUpdateTimestamp, messageList);
                } else {
                    throw new ClientException(ResponseCode.NO_MESSAGE, "query message by key finished, but no message.");
                }
            }
        }

        throw new ClientException(ResponseCode.TOPIC_NOT_EXIST, "The topic[" + topic + "] not matched route info");
    }


    public ExtMessage queryMessageByUniqueKey(String topic, String uniqueKey) throws InterruptedException, ClientException {
        QueryResult queryResult = queryMessage(topic, uniqueKey, 32, MessageClientIDSetter.getNearlyTimeFromID(uniqueKey).getTime() - 1000, Long.MAX_VALUE, true);
        if (queryResult != null && queryResult.getMessageList() != null && !queryResult.getMessageList().isEmpty()) {
            return queryResult.getMessageList().get(0);
        } else {
            return null;
        }
    }
}
