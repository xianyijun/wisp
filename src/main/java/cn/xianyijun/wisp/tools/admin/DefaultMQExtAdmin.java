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
        return null;
    }

    @Override
    public TopicList fetchAllTopicList() throws RemotingException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public TopicList fetchTopicsByCLuster(String clusterName) throws RemotingException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public KVTable fetchBrokerRuntimeStats(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup, String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo() throws InterruptedException, BrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return null;
    }

    @Override
    public TopicRouteData examineTopicRouteInfo(String topic) throws RemotingException, ClientException, InterruptedException {
        return extAdminDelegate.examineTopicRouteInfo(topic);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException, RemotingException, ClientException {
        return null;
    }

    @Override
    public ProducerConnection examineProducerConnectionInfo(String producerGroup, String topic) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public List<String> getNameServerAddressList() {
        return null;
    }

    @Override
    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName) throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, ClientException {
        return 0;
    }

    @Override
    public void putKVConfig(String namespace, String key, String value) {

    }

    @Override
    public String getKVConfig(String namespace, String key) throws RemotingException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public KVTable getKVListByNamespace(String namespace) throws RemotingException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public void deleteTopicInNameServer(HashSet<String> addrs, String topic) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public void createAndUpdateKvConfig(String namespace, String key, String value) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public void deleteKvConfig(String namespace, String key) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp, boolean force) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return null;
    }

    @Override
    public Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return null;
    }

    @Override
    public void resetOffsetNew(String consumerGroup, String topic, long timestamp) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group, String clientAddr) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return null;
    }

    @Override
    public void createOrUpdateOrderConf(String key, String value, boolean isCluster) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }

    @Override
    public GroupList queryTopicConsumeByWho(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException, RemotingException, ClientException {
        return null;
    }

    @Override
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic, String group) throws InterruptedException, BrokerException, RemotingException, ClientException {
        return null;
    }

    @Override
    public boolean cleanExpiredConsumerQueue(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return false;
    }

    @Override
    public boolean cleanExpiredConsumerQueueByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return false;
    }

    @Override
    public boolean cleanUnusedTopic(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return false;
    }

    @Override
    public boolean cleanUnusedTopicByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return false;
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack) throws RemotingException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String msgId) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic, String msgId) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public List<MessageTrack> messageTrackDetail(ExtMessage msg) throws RemotingException, ClientException, InterruptedException, BrokerException {
        return null;
    }

    @Override
    public void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline) throws RemotingException, ClientException, InterruptedException, BrokerException {

    }

    @Override
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public Set<String> getClusterList(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException, InterruptedException {
        return null;
    }

    @Override
    public Set<String> getTopicClusterList(String topic) throws InterruptedException, BrokerException, ClientException, RemotingException {
        return null;
    }

    @Override
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, BrokerException {
        return null;
    }

    @Override
    public TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, BrokerException {
        return null;
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq, long offset) throws RemotingException, InterruptedException, BrokerException {

    }

    @Override
    public void updateNameServerConfig(Properties properties, List<String> nameServers) throws InterruptedException, RemotingConnectException, UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException, ClientException, BrokerException {

    }

    @Override
    public Map<String, Properties> getNameServerConfig(List<String> nameServers) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr, String topic, int queueId, long index, int count, String consumerGroup) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException {
        return null;
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
