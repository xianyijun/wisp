package cn.xianyijun.wisp.tools.admin;

import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.admin.MQAdminExtInner;
import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.admin.ConsumeStats;
import cn.xianyijun.wisp.common.admin.OffsetWrapper;
import cn.xianyijun.wisp.common.admin.RollbackStats;
import cn.xianyijun.wisp.common.admin.TopicOffset;
import cn.xianyijun.wisp.common.admin.TopicStatsTable;
import cn.xianyijun.wisp.common.message.ExtClientMessage;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.namesrv.NameServerUtils;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.body.BrokerStatsData;
import cn.xianyijun.wisp.common.protocol.body.ClusterInfo;
import cn.xianyijun.wisp.common.protocol.body.ConsumeMessageDirectlyResult;
import cn.xianyijun.wisp.common.protocol.body.ConsumeStatsList;
import cn.xianyijun.wisp.common.protocol.body.ConsumerConnection;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.body.GroupList;
import cn.xianyijun.wisp.common.protocol.body.KVTable;
import cn.xianyijun.wisp.common.protocol.body.ProducerConnection;
import cn.xianyijun.wisp.common.protocol.body.QueryConsumeQueueResponseBody;
import cn.xianyijun.wisp.common.protocol.body.QueueTimeSpan;
import cn.xianyijun.wisp.common.protocol.body.SubscriptionGroupWrapper;
import cn.xianyijun.wisp.common.protocol.body.TopicConfigSerializeWrapper;
import cn.xianyijun.wisp.common.protocol.body.TopicList;
import cn.xianyijun.wisp.common.protocol.header.UpdateConsumerOffsetRequestHeader;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.common.protocol.route.BrokerData;
import cn.xianyijun.wisp.common.protocol.route.QueueData;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.common.subscription.SubscriptionGroupConfig;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.tools.admin.api.MessageTrack;
import cn.xianyijun.wisp.tools.admin.api.TrackType;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ExtAdminDelegate implements MQExtAdmin, MQAdminExtInner {

    private final DefaultMQExtAdmin extAdmin;
    private ServiceState serviceState = ServiceState.CREATE_JUST;
    private ClientFactory clientFactory;
    private RPCHook rpcHook;
    private long timeoutMillis = 20000;

    private Random random = new Random();

    public ExtAdminDelegate(DefaultMQExtAdmin extAdmin, RPCHook rpcHook, long timeoutMillis) {
        this.extAdmin = extAdmin;
        this.rpcHook = rpcHook;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void start() throws ClientException {
        switch (serviceState){
            case CREATE_JUST:
                this.serviceState = ServiceState.START_FAILED;
                this.extAdmin.changeInstanceNameToPID();
                this.clientFactory = MQClientManager.getInstance().getAndCreateMQClientInstance(this.extAdmin, rpcHook);
                boolean registerOK = clientFactory.registerExtAdmin(this.extAdmin.getAdminExtGroup(), this);

                if (!registerOK){
                    this.serviceState = ServiceState.CREATE_JUST;
                    throw new ClientException("The adminExt group[" + this.extAdmin.getAdminExtGroup()
                            + "] has created already, specifed another name please.",null);
                }

                clientFactory.start();

                log.info("this extAdmin [{}] start ok ",this.extAdmin.getAdminExtGroup());
                this.serviceState = ServiceState.RUNNING;

                break;

            case RUNNING:
            case START_FAILED:
            case SHUTDOWN_ALREADY:
                throw new ClientException("The AdminExt service state not OK, maybe started once, "
                        + this.serviceState,null);
            default:break;
        }
    }

    @Override
    public void shutdown() {
        switch (this.serviceState) {
            case CREATE_JUST:
                break;
            case RUNNING:
                this.clientFactory.unregisterExtAdmin(this.extAdmin.getAdminExtGroup());
                this.clientFactory.shutdown();

                log.info("the adminExt [{}] shutdown OK", this.extAdmin.getAdminExtGroup());
                this.serviceState = ServiceState.SHUTDOWN_ALREADY;
                break;
            case SHUTDOWN_ALREADY:
                break;
            default:
                break;
        }
    }

    @Override
    public void updateBrokerConfig(String brokerAddr, Properties properties) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, BrokerException {
        this.clientFactory.getClient().updateBrokerConfig(brokerAddr, properties, timeoutMillis);
    }

    @Override
    public Properties getBrokerConfig(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, BrokerException {
        return clientFactory.getClient().getBrokerConfig(brokerAddr, timeoutMillis);
    }

    @Override
    public void createAndUpdateTopicConfig(String addr, TopicConfig config) throws RemotingException, InterruptedException, ClientException {
        this.clientFactory.getClient().createTopic(addr, this.extAdmin.getCreateTopicKey(), config, timeoutMillis);
    }

    @Override
    public void createAndUpdateSubscriptionGroupConfig(String addr, SubscriptionGroupConfig config) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.clientFactory.getClient().createSubscriptionGroup(addr, config, timeoutMillis);
    }

    @Override
    public SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group) {
        return null;
    }

    @Override
    public TopicConfig examineTopicConfig(String addr, String topic) {
        return null;
    }

    @Override
    public TopicStatsTable examineTopicStats(String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);
        TopicStatsTable topicStatsTable = new TopicStatsTable();

        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            String addr = bd.selectBrokerAddr();
            if (addr != null) {
                TopicStatsTable tst = this.clientFactory.getClient().getTopicStatsInfo(addr, topic, timeoutMillis);
                topicStatsTable.getOffsetTable().putAll(tst.getOffsetTable());
            }
        }

        if (topicStatsTable.getOffsetTable().isEmpty()) {
            throw new ClientException("Not found the topic stats info", null);
        }

        return topicStatsTable;
    }

    @Override
    public TopicList fetchAllTopicList() throws RemotingException, ClientException, InterruptedException {
        return this.clientFactory.getClient().getTopicListFromNameServer(timeoutMillis);
    }

    @Override
    public TopicList fetchTopicsByCLuster(String clusterName) throws RemotingException, ClientException, InterruptedException {
        return this.clientFactory.getClient().getTopicsByCluster(clusterName, timeoutMillis);
    }

    @Override
    public KVTable fetchBrokerRuntimeStats(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException {
        return this.clientFactory.getClient().getBrokerRuntimeInfo(brokerAddr, timeoutMillis);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return examineConsumeStats(consumerGroup, null);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup, String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        String retryTopic = MixAll.getRetryTopic(consumerGroup);
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(retryTopic);
        ConsumeStats result = new ConsumeStats();

        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            String addr = bd.selectBrokerAddr();
            if (addr != null) {
                ConsumeStats consumeStats =
                        this.clientFactory.getClient().getConsumeStats(addr, consumerGroup, topic, timeoutMillis * 3);
                result.getOffsetTable().putAll(consumeStats.getOffsetTable());
                double value = result.getConsumeTps() + consumeStats.getConsumeTps();
                result.setConsumeTps(value);
            }
        }

        if (result.getOffsetTable().isEmpty()) {
            throw new ClientException(ResponseCode.CONSUMER_NOT_ONLINE,
                    "Not found the consumer group consume stats, because return offset table is empty, maybe the consumer not consume any message");
        }

        return result;
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo() throws InterruptedException, BrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return this.clientFactory.getClient().getBrokerClusterInfo(timeoutMillis);
    }

    @Override
    public TopicRouteData examineTopicRouteInfo(String topic) throws RemotingException, ClientException, InterruptedException {
        return this.clientFactory.getClient().getTopicRouteInfoFromNameServer(topic, timeoutMillis);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup) throws InterruptedException, BrokerException, RemotingException, ClientException {
        ConsumerConnection result = new ConsumerConnection();
        String topic = MixAll.getRetryTopic(consumerGroup);
        List<BrokerData> brokers = this.examineTopicRouteInfo(topic).getBrokerDatas();
        BrokerData brokerData = brokers.get(random.nextInt(brokers.size()));
        String addr = null;
        if (brokerData != null) {
            addr = brokerData.selectBrokerAddr();
            if (StringUtils.isNotBlank(addr)) {
                result = this.clientFactory.getClient().getConsumerConnectionList(addr, consumerGroup, timeoutMillis);
            }
        }

        if (result.getConnectionSet().isEmpty()) {
            log.warn("the consumer group not online. brokerAddr={}, group={}", addr, consumerGroup);
            throw new ClientException(ResponseCode.CONSUMER_NOT_ONLINE, "Not found the consumer group connection");
        }

        return result;
    }

    @Override
    public ProducerConnection examineProducerConnectionInfo(String producerGroup, String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        ProducerConnection result = new ProducerConnection();
        List<BrokerData> brokers = this.examineTopicRouteInfo(topic).getBrokerDatas();
        BrokerData brokerData = brokers.get(random.nextInt(brokers.size()));
        String addr = null;
        if (brokerData != null) {
            addr = brokerData.selectBrokerAddr();
            if (StringUtils.isNotBlank(addr)) {
                result = this.clientFactory.getClient().getProducerConnectionList(addr, producerGroup, timeoutMillis);
            }
        }

        if (result.getConnectionSet().isEmpty()) {
            log.warn("the producer group not online. brokerAddr={}, group={}", addr, producerGroup);
            throw new ClientException("Not found the producer group connection", null);
        }

        return result;
    }

    @Override
    public List<String> getNameServerAddressList() {
        return this.clientFactory.getClient().getNameServerAddressList();
    }

    @Override
    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, ClientException {
        return this.clientFactory.getClient().wipeWritePermOfBroker(namesrvAddr, brokerName, timeoutMillis);
    }

    @Override
    public void putKVConfig(String namespace, String key, String value) {

    }

    @Override
    public String getKVConfig(String namespace, String key) throws RemotingException, ClientException, InterruptedException {
        return this.clientFactory.getClient().getKVConfigValue(namespace, key, timeoutMillis);
    }

    @Override
    public KVTable getKVListByNamespace(String namespace) throws RemotingException, ClientException, InterruptedException {
        return this.clientFactory.getClient().getKVListByNamespace(namespace, timeoutMillis);
    }

    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic) throws RemotingException, BrokerException, InterruptedException, ClientException {
        for (String addr : addrs) {
            this.clientFactory.getClient().deleteTopicInBroker(addr, topic, timeoutMillis);
        }
    }

    @Override
    public void deleteTopicInNameServer(HashSet<String> addrs, String topic) throws RemotingException, InterruptedException, ClientException {
        if (addrs == null) {
            String ns = this.clientFactory.getClient().fetchNameServerAddr();
            addrs = new HashSet<>(Arrays.asList(ns.split(";")));
        }
        for (String addr : addrs) {
            this.clientFactory.getClient().deleteTopicInNameServer(addr, topic, timeoutMillis);
        }
    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName) throws RemotingException, InterruptedException, ClientException {
        this.clientFactory.getClient().deleteSubscriptionGroup(addr, groupName, timeoutMillis);
    }

    @Override
    public void createAndUpdateKvConfig(String namespace, String key, String value) throws RemotingException, InterruptedException, ClientException {
        this.clientFactory.getClient().putKVConfigValue(namespace, key, value, timeoutMillis);
    }

    @Override
    public void deleteKvConfig(String namespace, String key) throws RemotingException, InterruptedException, ClientException {
        this.clientFactory.getClient().deleteKVConfigValue(namespace, key, timeoutMillis);
    }

    @Override
    public List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp, boolean force) throws RemotingException, BrokerException, InterruptedException, ClientException {
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);
        List<RollbackStats> rollbackStatsList = new ArrayList<>();
        Map<String, Integer> topicRouteMap = new HashMap<>();
        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            for (QueueData queueData : topicRouteData.getQueueDatas()) {
                topicRouteMap.put(bd.selectBrokerAddr(), queueData.getReadQueueNums());
            }
        }
        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            String addr = bd.selectBrokerAddr();
            if (addr != null) {
                ConsumeStats consumeStats = this.clientFactory.getClient().getConsumeStats(addr, consumerGroup, timeoutMillis);

                boolean hasConsumed = false;
                for (Map.Entry<MessageQueue, OffsetWrapper> entry : consumeStats.getOffsetTable().entrySet()) {
                    MessageQueue queue = entry.getKey();
                    OffsetWrapper offsetWrapper = entry.getValue();
                    if (topic.equals(queue.getTopic())) {
                        hasConsumed = true;
                        RollbackStats rollbackStats = resetOffsetConsumeOffset(addr, consumerGroup, queue, offsetWrapper, timestamp, force);
                        rollbackStatsList.add(rollbackStats);
                    }
                }

                if (!hasConsumed) {
                    HashMap<MessageQueue, TopicOffset> topicStatus =
                            this.clientFactory.getClient().getTopicStatsInfo(addr, topic, timeoutMillis).getOffsetTable();
                    for (int i = 0; i < topicRouteMap.get(addr); i++) {
                        MessageQueue queue = new MessageQueue(topic, bd.getBrokerName(), i);
                        OffsetWrapper offsetWrapper = new OffsetWrapper();
                        offsetWrapper.setBrokerOffset(topicStatus.get(queue).getMaxOffset());
                        offsetWrapper.setConsumerOffset(topicStatus.get(queue).getMinOffset());

                        RollbackStats rollbackStats = resetOffsetConsumeOffset(addr, consumerGroup, queue, offsetWrapper, timestamp, force);
                        rollbackStatsList.add(rollbackStats);
                    }
                }
            }
        }
        return rollbackStatsList;
    }

    private RollbackStats resetOffsetConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue queue,
                                                   OffsetWrapper offsetWrapper,
                                                   long timestamp, boolean force) throws RemotingException, InterruptedException, BrokerException {
        long resetOffset;
        if (timestamp == -1) {

            resetOffset = this.clientFactory.getClient().getMaxOffset(brokerAddr, queue.getTopic(), queue.getQueueId(), timeoutMillis);
        } else {
            resetOffset =
                    this.clientFactory.getClient().searchOffset(brokerAddr, queue.getTopic(), queue.getQueueId(), timestamp,
                            timeoutMillis);
        }

        RollbackStats rollbackStats = new RollbackStats();
        rollbackStats.setBrokerName(queue.getBrokerName());
        rollbackStats.setQueueId(queue.getQueueId());
        rollbackStats.setBrokerOffset(offsetWrapper.getBrokerOffset());
        rollbackStats.setConsumerOffset(offsetWrapper.getConsumerOffset());
        rollbackStats.setTimestampOffset(resetOffset);
        rollbackStats.setRollbackOffset(offsetWrapper.getConsumerOffset());

        if (force || resetOffset <= offsetWrapper.getConsumerOffset()) {
            rollbackStats.setRollbackOffset(resetOffset);
            UpdateConsumerOffsetRequestHeader requestHeader = new UpdateConsumerOffsetRequestHeader();
            requestHeader.setConsumerGroup(consumeGroup);
            requestHeader.setTopic(queue.getTopic());
            requestHeader.setQueueId(queue.getQueueId());
            requestHeader.setCommitOffset(resetOffset);
            this.clientFactory.getClient().updateConsumerOffset(brokerAddr, requestHeader, timeoutMillis);
        }
        return rollbackStats;
    }

    @Override
    public Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce) throws RemotingException, InterruptedException, ClientException {
        return resetOffsetByTimestamp(topic, group, timestamp, isForce, false);
    }

    private Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce,
                                                           boolean isC)
            throws RemotingException, InterruptedException, ClientException {
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);
        List<BrokerData> brokerDatas = topicRouteData.getBrokerDatas();
        Map<MessageQueue, Long> allOffsetTable = new HashMap<>();
        if (brokerDatas != null) {
            for (BrokerData brokerData : brokerDatas) {
                String addr = brokerData.selectBrokerAddr();
                if (addr != null) {
                    Map<MessageQueue, Long> offsetTable =
                            this.clientFactory.getClient().invokeBrokerToResetOffset(addr, topic, group, timestamp, isForce,
                                    timeoutMillis, isC);
                    if (offsetTable != null) {
                        allOffsetTable.putAll(offsetTable);
                    }
                }
            }
        }
        return allOffsetTable;
    }

    @Override
    public void resetOffsetNew(String consumerGroup, String topic, long timestamp) throws RemotingException, BrokerException, InterruptedException, ClientException {
        try {
            this.resetOffsetByTimestamp(topic, consumerGroup, timestamp, true);
        } catch (ClientException e) {
            if (ResponseCode.CONSUMER_NOT_ONLINE == e.getResponseCode()) {
                this.resetOffsetByTimestampOld(consumerGroup, topic, timestamp, true);
                return;
            }
            throw e;
        }
    }

    @Override
    public Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group, String clientAddr) throws RemotingException, InterruptedException, ClientException {
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);
        List<BrokerData> brokerDatas = topicRouteData.getBrokerDatas();
        if (brokerDatas != null && brokerDatas.size() > 0) {
            String addr = brokerDatas.get(0).selectBrokerAddr();
            if (addr != null) {
                return this.clientFactory.getClient().invokeBrokerToGetConsumerStatus(addr, topic, group, clientAddr, timeoutMillis);
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public void createOrUpdateOrderConf(String key, String value, boolean isCluster) throws RemotingException, InterruptedException, ClientException {
        if (isCluster) {
            this.clientFactory.getClient()
                    .putKVConfigValue(NameServerUtils.NAMESPACE_ORDER_TOPIC_CONFIG, key, value, timeoutMillis);
        } else {
            String oldOrderConfs = null;
            try {
                oldOrderConfs =
                        this.clientFactory.getClient().getKVConfigValue(NameServerUtils.NAMESPACE_ORDER_TOPIC_CONFIG, key,
                                timeoutMillis);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Map<String, String> orderConfMap = new HashMap<String, String>();
            if (!StringUtils.isBlank(oldOrderConfs)) {
                String[] oldOrderConfArr = oldOrderConfs.split(";");
                for (String oldOrderConf : oldOrderConfArr) {
                    String[] items = oldOrderConf.split(":");
                    orderConfMap.put(items[0], oldOrderConf);
                }
            }
            String[] items = value.split(":");
            orderConfMap.put(items[0], value);

            StringBuilder newOrderConf = new StringBuilder();
            String splitor = "";
            for (Map.Entry<String, String> entry : orderConfMap.entrySet()) {
                newOrderConf.append(splitor).append(entry.getValue());
                splitor = ";";
            }
            this.clientFactory.getClient().putKVConfigValue(NameServerUtils.NAMESPACE_ORDER_TOPIC_CONFIG, key,
                    newOrderConf.toString(), timeoutMillis);
        }
    }

    @Override
    public GroupList queryTopicConsumeByWho(String topic) throws InterruptedException, BrokerException, RemotingException, ClientException {
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);

        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            String addr = bd.selectBrokerAddr();
            if (addr != null) {
                return this.clientFactory.getClient().queryTopicConsumeByWho(addr, topic, timeoutMillis);
            }
            break;
        }
        return null;
    }

    @Override
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic, String group) throws InterruptedException, BrokerException, RemotingException, ClientException {
        List<QueueTimeSpan> spanSet = new ArrayList<>();
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);
        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            String addr = bd.selectBrokerAddr();
            if (addr != null) {
                spanSet.addAll(this.clientFactory.getClient().queryConsumeTimeSpan(addr, topic, group, timeoutMillis));
            }
        }
        return spanSet;
    }

    @Override
    public boolean cleanExpiredConsumerQueue(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, ClientException {
        boolean result = false;
        try {
            ClusterInfo clusterInfo = examineBrokerClusterInfo();
            if (null == cluster || "".equals(cluster)) {
                for (String targetCluster : clusterInfo.retrieveAllClusterNames()) {
                    result = cleanExpiredConsumerQueueByCluster(clusterInfo, targetCluster);
                }
            } else {
                result = cleanExpiredConsumerQueueByCluster(clusterInfo, cluster);
            }
        } catch (BrokerException e) {
            log.error("cleanExpiredConsumerQueue error.", e);
        }

        return result;
    }

    public boolean cleanExpiredConsumerQueueByCluster(ClusterInfo clusterInfo,
                                                      String cluster) throws InterruptedException, RemotingTimeoutException, ClientException, RemotingSendRequestException, RemotingConnectException {
        boolean result = false;
        String[] addrs = clusterInfo.retrieveAllAddrByCluster(cluster);
        for (String addr : addrs) {
            result = cleanExpiredConsumerQueueByAddr(addr);
        }
        return result;
    }


    @Override
    public boolean cleanExpiredConsumerQueueByAddr(String addr) throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, ClientException {
        boolean result = this.clientFactory.getClient().cleanExpiredConsumeQueue(addr, timeoutMillis);
        log.warn("clean expired ConsumeQueue on target " + addr + " broker " + result);
        return result;
    }

    @Override
    public boolean cleanUnusedTopic(String cluster) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException {
        boolean result = false;
        try {
            ClusterInfo clusterInfo = examineBrokerClusterInfo();
            if (null == cluster || "".equals(cluster)) {
                for (String targetCluster : clusterInfo.retrieveAllClusterNames()) {
                    result = cleanUnusedTopicByCluster(clusterInfo, targetCluster);
                }
            } else {
                result = cleanUnusedTopicByCluster(clusterInfo, cluster);
            }
        } catch (BrokerException e) {
            log.error("cleanExpiredConsumerQueue error.", e);
        }

        return result;
    }

    private boolean cleanUnusedTopicByCluster(ClusterInfo clusterInfo, String cluster) throws InterruptedException, RemotingTimeoutException, ClientException, RemotingSendRequestException, RemotingConnectException {
        boolean result = false;
        String[] addrs = clusterInfo.retrieveAllAddrByCluster(cluster);
        for (String addr : addrs) {
            result = cleanUnusedTopicByAddr(addr);
        }
        return result;
    }

    @Override
    public boolean cleanUnusedTopicByAddr(String addr) throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, ClientException {
        boolean result = this.clientFactory.getClient().cleanUnusedTopicByAddr(addr, timeoutMillis);
        log.warn("clean expired ConsumeQueue on target " + addr + " broker " + result);
        return result;
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack) throws RemotingException, ClientException, InterruptedException {
        String topic = MixAll.RETRY_GROUP_TOPIC_PREFIX + consumerGroup;
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(topic);
        List<BrokerData> brokerDatas = topicRouteData.getBrokerDatas();
        if (brokerDatas != null) {
            for (BrokerData brokerData : brokerDatas) {
                String addr = brokerData.selectBrokerAddr();
                if (addr != null) {
                    return this.clientFactory.getClient().getConsumerRunningInfo(addr, consumerGroup, clientId, jstack,
                            timeoutMillis * 3);
                }
            }
        }
        return null;
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String msgId) throws InterruptedException, BrokerException, RemotingException, ClientException {
        ExtMessage msg = this.viewMessage(msgId);

        return this.clientFactory.getClient().consumeMessageDirectly(RemotingUtils.socketAddress2String(msg.getStoreHost()),
                consumerGroup, clientId, msgId, timeoutMillis * 3);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic, String msgId) throws ClientException, InterruptedException, RemotingException {
        ExtMessage msg = this.viewMessage(topic, msgId);
        if (msg.getProperty(MessageConst.PROPERTY_UNIQUE_CLIENT_MESSAGE_ID_KEYIDX) == null) {
            return this.clientFactory.getClient().consumeMessageDirectly(RemotingUtils.socketAddress2String(msg.getStoreHost()),
                    consumerGroup, clientId, msgId, timeoutMillis * 3);
        } else {
            ExtClientMessage msgClient = (ExtClientMessage) msg;
            return this.clientFactory.getClient().consumeMessageDirectly(RemotingUtils.socketAddress2String(msg.getStoreHost()),
                    consumerGroup, clientId, msgClient.getOffsetMsgId(), timeoutMillis * 3);
        }
    }

    @Override
    public List<MessageTrack> messageTrackDetail(ExtMessage msg) throws InterruptedException, BrokerException, RemotingException, ClientException {
        List<MessageTrack> result = new ArrayList<MessageTrack>();

        GroupList groupList = this.queryTopicConsumeByWho(msg.getTopic());

        for (String group : groupList.getGroupList()) {

            MessageTrack mt = new MessageTrack();
            mt.setConsumerGroup(group);
            mt.setTrackType(TrackType.UNKNOWN);
            ConsumerConnection cc;
            try {
                cc = this.examineConsumerConnectionInfo(group);
            } catch (BrokerException e) {
                if (ResponseCode.CONSUMER_NOT_ONLINE == e.getResponseCode()) {
                    mt.setTrackType(TrackType.NOT_ONLINE);
                }
                mt.setExceptionDesc("CODE:" + e.getResponseCode() + " DESC:" + e.getErrorMessage());
                result.add(mt);
                continue;
            } catch (Exception e) {
                mt.setExceptionDesc(RemotingHelper.exceptionSimpleDesc(e));
                result.add(mt);
                continue;
            }

            switch (cc.getConsumeType()) {
                case CONSUME_ACTIVELY:
                    mt.setTrackType(TrackType.PULL);
                    break;
                case CONSUME_PASSIVELY:
                    boolean ifConsumed;
                    try {
                        ifConsumed = this.consumed(msg, group);
                    } catch (ClientException e) {
                        if (ResponseCode.CONSUMER_NOT_ONLINE == e.getResponseCode()) {
                            mt.setTrackType(TrackType.NOT_ONLINE);
                        }
                        mt.setExceptionDesc("CODE:" + e.getResponseCode() + " DESC:" + e.getErrorMessage());
                        result.add(mt);
                        continue;
                    } catch (BrokerException e) {
                        if (ResponseCode.CONSUMER_NOT_ONLINE == e.getResponseCode()) {
                            mt.setTrackType(TrackType.NOT_ONLINE);
                        }
                        mt.setExceptionDesc("CODE:" + e.getResponseCode() + " DESC:" + e.getErrorMessage());
                        result.add(mt);
                        continue;
                    } catch (Exception e) {
                        mt.setExceptionDesc(RemotingHelper.exceptionSimpleDesc(e));
                        result.add(mt);
                        continue;
                    }

                    if (ifConsumed) {
                        mt.setTrackType(TrackType.CONSUMED);
                        for (Map.Entry<String, SubscriptionData> next : cc.getSubscriptionTable().entrySet()) {
                            if (next.getKey().equals(msg.getTopic())) {
                                if (next.getValue().getTagsSet().contains(msg.getTags())
                                        || next.getValue().getTagsSet().contains("*")
                                        || next.getValue().getTagsSet().isEmpty()) {
                                } else {
                                    mt.setTrackType(TrackType.CONSUMED_BUT_FILTERED);
                                }
                            }
                        }
                    } else {
                        mt.setTrackType(TrackType.NOT_CONSUME_YET);
                    }
                    break;
                default:
                    break;
            }
            result.add(mt);
        }
        return result;
    }

    private boolean consumed(final ExtMessage msg,
                             final String group) throws RemotingException, ClientException, InterruptedException,
            BrokerException {

        ConsumeStats consumeStats = this.examineConsumeStats(group);

        ClusterInfo ci = this.examineBrokerClusterInfo();

        for (Map.Entry<MessageQueue, OffsetWrapper> next : consumeStats.getOffsetTable().entrySet()) {
            MessageQueue mq = next.getKey();
            if (mq.getTopic().equals(msg.getTopic()) && mq.getQueueId() == msg.getQueueId()) {
                BrokerData brokerData = ci.getBrokerAddrTable().get(mq.getBrokerName());
                if (brokerData != null) {
                    String addr = brokerData.getBrokerAddrs().get(MixAll.MASTER_ID);
                    if (addr.equals(RemotingUtils.socketAddress2String(msg.getStoreHost()))) {
                        if (next.getValue().getConsumerOffset() > msg.getQueueOffset()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline) throws RemotingException, ClientException, InterruptedException {
        String retryTopic = MixAll.getRetryTopic(srcGroup);
        TopicRouteData topicRouteData = this.examineTopicRouteInfo(retryTopic);

        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
            String addr = bd.selectBrokerAddr();
            if (addr != null) {
                this.clientFactory.getClient().cloneGroupOffset(addr, srcGroup, destGroup, topic, isOffline, timeoutMillis * 3);
            }
        }
    }

    @Override
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey) throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, ClientException {
        return this.clientFactory.getClient().viewBrokerStatsData(brokerAddr, statsName, statsKey, timeoutMillis);
    }

    @Override
    public Set<String> getClusterList(String topic) {
        return this.clientFactory.getClient().getClusterList(topic, timeoutMillis);
    }

    @Override
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, ClientException {
        return this.clientFactory.getClient().fetchConsumeStatsInBroker(brokerAddr, isOrder, timeoutMillis);
    }

    @Override
    public Set<String> getTopicClusterList(String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        Set<String> clusterSet = new HashSet<String>();
        ClusterInfo clusterInfo = examineBrokerClusterInfo();
        TopicRouteData topicRouteData = examineTopicRouteInfo(topic);
        BrokerData brokerData = topicRouteData.getBrokerDatas().get(0);
        String brokerName = brokerData.getBrokerName();
        for (Map.Entry<String, Set<String>> next : clusterInfo.getClusterAddrTable().entrySet()) {
            if (next.getValue().contains(brokerName)) {
                clusterSet.add(next.getKey());
            }
        }
        return clusterSet;
    }

    @Override
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, BrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return this.clientFactory.getClient().getAllSubscriptionGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, BrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return this.clientFactory.getClient().getAllTopicConfig(brokerAddr, timeoutMillis);
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq, long offset) throws RemotingException, InterruptedException, BrokerException {
        UpdateConsumerOffsetRequestHeader requestHeader = new UpdateConsumerOffsetRequestHeader();
        requestHeader.setConsumerGroup(consumeGroup);
        requestHeader.setTopic(mq.getTopic());
        requestHeader.setQueueId(mq.getQueueId());
        requestHeader.setCommitOffset(offset);
        this.clientFactory.getClient().updateConsumerOffset(brokerAddr, requestHeader, timeoutMillis);
    }

    @Override
    public void updateNameServerConfig(Properties properties, List<String> nameServers) throws InterruptedException, RemotingConnectException, UnsupportedEncodingException, RemotingTimeoutException, ClientException, RemotingSendRequestException, BrokerException {
        this.clientFactory.getClient().updateNameServerConfig(properties, nameServers, timeoutMillis);
    }

    @Override
    public Map<String, Properties> getNameServerConfig(List<String> nameServers) throws InterruptedException, RemotingConnectException, ClientException, RemotingTimeoutException, UnsupportedEncodingException, RemotingSendRequestException {
        return this.clientFactory.getClient().getNameServerConfig(nameServers, timeoutMillis);
    }

    @Override
    public QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr, String topic, int queueId, long index, int count, String consumerGroup) throws InterruptedException, RemotingTimeoutException, ClientException, RemotingSendRequestException, RemotingConnectException {
        return this.clientFactory.getClient().queryConsumeQueue(
                brokerAddr, topic, queueId, index, count, consumerGroup, timeoutMillis
        );
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        this.clientFactory.getAdmin().createTopic(key, newTopic, queueNum , topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        return this.clientFactory.getAdmin().searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws ClientException {
        return this.clientFactory.getAdmin().maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws ClientException {
        return this.clientFactory.getAdmin().minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        return this.clientFactory.getAdmin().earliestMsgStoreTime(mq);
    }

    @Override
    public ExtMessage viewMessage(String messageId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.clientFactory.getAdmin().viewMessage(messageId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws ClientException, InterruptedException {
        return this.clientFactory.getAdmin().queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    public ExtMessage viewMessage(String topic, String messageId) throws InterruptedException, ClientException {
        try {
            MessageDecoder.decodeMessageId(messageId);
            return this.viewMessage(messageId);
        } catch (Exception e) {
            log.warn("the msgId maybe created by new client. msgId={}", messageId, e);
        }
        return this.clientFactory.getAdmin().queryMessageByUniqueKey(topic, messageId);
    }
}
