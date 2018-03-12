package cn.xianyijun.wisp.tools.admin;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.admin.ConsumeStats;
import cn.xianyijun.wisp.common.admin.RollbackStats;
import cn.xianyijun.wisp.common.admin.TopicStatsTable;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
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
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.common.subscription.SubscriptionGroupConfig;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingCommandException;
import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.tools.admin.api.MessageTrack;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * @author xianyijun
 */

@Getter
public class DefaultMQExtAdmin extends ClientConfig implements MQExtAdmin {
    private final ExtAdminDelegate extAdminDelegate;
    private String adminExtGroup = "admin_ext_group";
    private String createTopicKey = MixAll.DEFAULT_TOPIC;
    private long timeoutMillis = 5000;

    public DefaultMQExtAdmin() {
        this.extAdminDelegate = new ExtAdminDelegate(this, null,timeoutMillis);
    }

    @Override
    public void start() throws ClientException {
        extAdminDelegate.start();
    }

    @Override
    public void shutdown() {
        extAdminDelegate.shutdown();
    }

    @Override
    public void updateBrokerConfig(String brokerAddr, Properties properties) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, BrokerException {
        this.extAdminDelegate.updateBrokerConfig(brokerAddr, properties);
    }

    @Override
    public Properties getBrokerConfig(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, BrokerException {
        return this.extAdminDelegate.getBrokerConfig(brokerAddr);
    }

    @Override
    public void createAndUpdateTopicConfig(String addr, TopicConfig config) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.createAndUpdateTopicConfig(addr, config);
    }

    @Override
    public void createAndUpdateSubscriptionGroupConfig(String addr, SubscriptionGroupConfig config) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.createAndUpdateSubscriptionGroupConfig(addr, config);
    }

    @Override
    public SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group) {
        return this.extAdminDelegate.examineSubscriptionGroupConfig(addr, group);
    }

    @Override
    public TopicConfig examineTopicConfig(String addr, String topic) {
        return this.extAdminDelegate.examineTopicConfig(addr, topic);
    }

    @Override
    public TopicStatsTable examineTopicStats(String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.examineTopicStats(topic);
    }

    @Override
    public TopicList fetchAllTopicList() throws RemotingException, ClientException, InterruptedException {
        return this.extAdminDelegate.fetchAllTopicList();
    }

    @Override
    public TopicList fetchTopicsByCLuster(String clusterName) throws RemotingException, ClientException, InterruptedException {
        return this.extAdminDelegate.fetchTopicsByCLuster(clusterName);
    }

    @Override
    public KVTable fetchBrokerRuntimeStats(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException {
        return this.extAdminDelegate.fetchBrokerRuntimeStats(brokerAddr);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.examineConsumeStats(consumerGroup);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup, String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.examineConsumeStats(consumerGroup, topic);
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo() throws InterruptedException, BrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return this.extAdminDelegate.examineBrokerClusterInfo();
    }

    @Override
    public TopicRouteData examineTopicRouteInfo(String topic) throws RemotingException, ClientException, InterruptedException {
        return extAdminDelegate.examineTopicRouteInfo(topic);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException, RemotingException, ClientException {
        return this.extAdminDelegate.examineConsumerConnectionInfo(consumerGroup);
    }

    @Override
    public ProducerConnection examineProducerConnectionInfo(String producerGroup, String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.examineProducerConnectionInfo(producerGroup, topic);
    }

    @Override
    public List<String> getNameServerAddressList() {
        return this.extAdminDelegate.getNameServerAddressList();
    }

    @Override
    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName) throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, ClientException {
        return this.extAdminDelegate.wipeWritePermOfBroker(namesrvAddr, brokerName);
    }

    @Override
    public void putKVConfig(String namespace, String key, String value) {
        this.extAdminDelegate.putKVConfig(namespace, key, value);
    }

    @Override
    public String getKVConfig(String namespace, String key) throws RemotingException, ClientException, InterruptedException {
        return this.extAdminDelegate.getKVConfig(namespace, key);
    }

    @Override
    public KVTable getKVListByNamespace(String namespace) throws RemotingException, ClientException, InterruptedException {
        return this.extAdminDelegate.getKVListByNamespace(namespace);
    }

    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.deleteTopicInBroker(addrs, topic);
    }

    @Override
    public void deleteTopicInNameServer(HashSet<String> addrs, String topic) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.deleteTopicInNameServer(addrs, topic);
    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.deleteSubscriptionGroup(addr, groupName);
    }

    @Override
    public void createAndUpdateKvConfig(String namespace, String key, String value) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.createAndUpdateKvConfig(namespace, key, value);
    }

    @Override
    public void deleteKvConfig(String namespace, String key) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.deleteKvConfig(namespace, key);
    }

    @Override
    public List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp, boolean force) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.extAdminDelegate.resetOffsetByTimestampOld(consumerGroup, topic, timestamp ,force);
    }

    @Override
    public Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.extAdminDelegate.resetOffsetByTimestamp(topic, group, timestamp ,isForce);
    }

    @Override
    public void resetOffsetNew(String consumerGroup, String topic, long timestamp) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.resetOffsetNew(consumerGroup, topic, timestamp);
    }

    @Override
    public Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group, String clientAddr) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.extAdminDelegate.getConsumeStatus(topic, group, clientAddr);
    }

    @Override
    public void createOrUpdateOrderConf(String key, String value, boolean isCluster) throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.extAdminDelegate.createOrUpdateOrderConf(key, value, isCluster);
    }

    @Override
    public GroupList queryTopicConsumeByWho(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException, RemotingException, ClientException {
        return this.extAdminDelegate.queryTopicConsumeByWho(topic);
    }

    @Override
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic, String group) throws InterruptedException, BrokerException, RemotingException, ClientException {
        return this.extAdminDelegate.queryConsumeTimeSpan(topic, group);
    }

    @Override
    public boolean cleanExpiredConsumerQueue(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.cleanExpiredConsumerQueue(cluster);
    }

    @Override
    public boolean cleanExpiredConsumerQueueByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.cleanExpiredConsumerQueueByAddr(addr);
    }

    @Override
    public boolean cleanUnusedTopic(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.cleanUnusedTopic(cluster);
    }

    @Override
    public boolean cleanUnusedTopicByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.cleanUnusedTopicByAddr(addr);
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack) throws RemotingException, ClientException, InterruptedException {
        return this.extAdminDelegate.getConsumerRunningInfo(consumerGroup, clientId, jstack);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String msgId) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.consumeMessageDirectly(consumerGroup, clientId, msgId);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic, String msgId) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.consumeMessageDirectly(consumerGroup, clientId, topic, msgId);
    }

    @Override
    public List<MessageTrack> messageTrackDetail(ExtMessage msg) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return this.extAdminDelegate.messageTrackDetail(msg);
    }

    @Override
    public void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline) throws RemotingException, ClientException, InterruptedException, BrokerException {
        this.extAdminDelegate.cloneGroupOffset(srcGroup, destGroup, topic ,isOffline);
    }

    @Override
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.viewBrokerStatsData(brokerAddr, statsName, statsKey);
    }

    @Override
    public Set<String> getClusterList(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.getClusterList(topic);
    }

    @Override
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return this.extAdminDelegate.fetchConsumeStatsInBroker(brokerAddr, isOrder, timeoutMillis);
    }

    @Override
    public Set<String> getTopicClusterList(String topic) throws InterruptedException, BrokerException, ClientException, RemotingException {
        return this.extAdminDelegate.getTopicClusterList(topic);
    }

    @Override
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, BrokerException {
        return this.extAdminDelegate.getAllSubscriptionGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, BrokerException {
        return this.extAdminDelegate.getAllTopicGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq, long offset) throws RemotingException, InterruptedException, BrokerException {
        this.extAdminDelegate.updateConsumeOffset(brokerAddr, consumeGroup, mq, offset);
    }

    @Override
    public void updateNameServerConfig(Properties properties, List<String> nameServers) throws InterruptedException, RemotingConnectException, UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException, ClientException, BrokerException {
        this.extAdminDelegate.updateNameServerConfig(properties, nameServers);
    }

    @Override
    public Map<String, Properties> getNameServerConfig(List<String> nameServers) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException, UnsupportedEncodingException {
        return this.extAdminDelegate.getNameServerConfig(nameServers);
    }

    @Override
    public QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr, String topic, int queueId, long index, int count, String consumerGroup) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException {
        return this.extAdminDelegate.queryConsumeQueue(brokerAddr, topic, queueId, index, count, consumerGroup);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        this.extAdminDelegate.createTopic(key , newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        return this.extAdminDelegate.searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws ClientException {
        return this.extAdminDelegate.maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws ClientException {
        return this.extAdminDelegate.minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        return this.extAdminDelegate.earliestMsgStoreTime(mq);
    }

    @Override
    public ExtMessage viewMessage(String offsetMsgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.extAdminDelegate.viewMessage(offsetMsgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws ClientException, InterruptedException {
        return this.extAdminDelegate.queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    public ExtMessage viewMessage(String topic, String msgId) throws InterruptedException, ClientException {
        return this.extAdminDelegate.viewMessage(topic, msgId);
    }
}
