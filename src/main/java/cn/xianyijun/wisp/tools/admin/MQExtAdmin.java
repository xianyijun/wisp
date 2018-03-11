package cn.xianyijun.wisp.tools.admin;

import cn.xianyijun.wisp.client.MQAdmin;
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

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The interface Mq ext admin.
 */
public interface MQExtAdmin extends MQAdmin {
    /**
     * Start.
     *
     * @throws ClientException the client exception
     */
    void start() throws ClientException;

    /**
     * Shutdown.
     */
    void shutdown();

    /**
     * Update broker config.
     *
     * @param brokerAddr the broker addr
     * @param properties the properties
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws InterruptedException         the interrupted exception
     * @throws BrokerException              the broker exception
     */
    void updateBrokerConfig(final String brokerAddr, final Properties properties) throws RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, BrokerException;

    /**
     * Gets broker config.
     *
     * @param brokerAddr the broker addr
     * @return the broker config
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws InterruptedException         the interrupted exception
     * @throws BrokerException              the broker exception
     */
    Properties getBrokerConfig(final String brokerAddr) throws RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, BrokerException;

    /**
     * Create and update topic config.
     *
     * @param addr   the addr
     * @param config the config
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void createAndUpdateTopicConfig(final String addr,
                                    final TopicConfig config) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Create and update subscription group config.
     *
     * @param addr   the addr
     * @param config the config
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void createAndUpdateSubscriptionGroupConfig(final String addr,
                                                final SubscriptionGroupConfig config) throws RemotingException,
            BrokerException, InterruptedException, ClientException;

    /**
     * Examine subscription group config subscription group config.
     *
     * @param addr  the addr
     * @param group the group
     * @return the subscription group config
     */
    SubscriptionGroupConfig examineSubscriptionGroupConfig(final String addr, final String group);

    /**
     * Examine topic config topic config.
     *
     * @param addr  the addr
     * @param topic the topic
     * @return the topic config
     */
    TopicConfig examineTopicConfig(final String addr, final String topic);

    /**
     * Examine topic stats topic stats table.
     *
     * @param topic the topic
     * @return the topic stats table
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    TopicStatsTable examineTopicStats(
            final String topic) throws RemotingException, ClientException, InterruptedException,
            BrokerException;

    /**
     * Fetch all topic list topic list.
     *
     * @return the topic list
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    TopicList fetchAllTopicList() throws RemotingException, ClientException, InterruptedException;

    /**
     * Fetch topics by c luster topic list.
     *
     * @param clusterName the cluster name
     * @return the topic list
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    TopicList fetchTopicsByCLuster(
            String clusterName) throws RemotingException, ClientException, InterruptedException;

    /**
     * Fetch broker runtime stats kv table.
     *
     * @param brokerAddr the broker addr
     * @return the kv table
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws InterruptedException         the interrupted exception
     * @throws BrokerException              the broker exception
     */
    KVTable fetchBrokerRuntimeStats(
            final String brokerAddr) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, InterruptedException, BrokerException;

    /**
     * Examine consume stats consume stats.
     *
     * @param consumerGroup the consumer group
     * @return the consume stats
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    ConsumeStats examineConsumeStats(
            final String consumerGroup) throws RemotingException, ClientException, InterruptedException,
            BrokerException;

    /**
     * Examine consume stats consume stats.
     *
     * @param consumerGroup the consumer group
     * @param topic         the topic
     * @return the consume stats
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    ConsumeStats examineConsumeStats(final String consumerGroup,
                                     final String topic) throws RemotingException, ClientException,
            InterruptedException, BrokerException;

    /**
     * Examine broker cluster info cluster info.
     *
     * @return the cluster info
     * @throws InterruptedException         the interrupted exception
     * @throws BrokerException              the broker exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingConnectException     the remoting connect exception
     */
    ClusterInfo examineBrokerClusterInfo() throws InterruptedException, BrokerException, RemotingTimeoutException,
            RemotingSendRequestException, RemotingConnectException;

    /**
     * Examine topic route info topic route data.
     *
     * @param topic the topic
     * @return the topic route data
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    TopicRouteData examineTopicRouteInfo(
            final String topic) throws RemotingException, ClientException, InterruptedException;

    /**
     * Examine consumer connection info consumer connection.
     *
     * @param consumerGroup the consumer group
     * @return the consumer connection
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws InterruptedException         the interrupted exception
     * @throws BrokerException              the broker exception
     * @throws RemotingException            the remoting exception
     * @throws ClientException              the client exception
     */
    ConsumerConnection examineConsumerConnectionInfo(final String consumerGroup) throws RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException, InterruptedException, BrokerException, RemotingException,
            ClientException;

    /**
     * Examine producer connection info producer connection.
     *
     * @param producerGroup the producer group
     * @param topic         the topic
     * @return the producer connection
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    ProducerConnection examineProducerConnectionInfo(final String producerGroup,
                                                     final String topic) throws RemotingException,
            ClientException, InterruptedException, BrokerException;

    /**
     * Gets name server address list.
     *
     * @return the name server address list
     */
    List<String> getNameServerAddressList();

    /**
     * Wipe write perm of broker int.
     *
     * @param namesrvAddr the namesrv addr
     * @param brokerName  the broker name
     * @return the int
     * @throws RemotingCommandException     the remoting command exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws InterruptedException         the interrupted exception
     * @throws ClientException              the client exception
     */
    int wipeWritePermOfBroker(final String namesrvAddr, String brokerName) throws RemotingCommandException,
            RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, ClientException;

    /**
     * Put kv config.
     *
     * @param namespace the namespace
     * @param key       the key
     * @param value     the value
     */
    void putKVConfig(final String namespace, final String key, final String value);

    /**
     * Gets kv config.
     *
     * @param namespace the namespace
     * @param key       the key
     * @return the kv config
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    String getKVConfig(final String namespace,
                       final String key) throws RemotingException, ClientException, InterruptedException;

    /**
     * Gets kv list by namespace.
     *
     * @param namespace the namespace
     * @return the kv list by namespace
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    KVTable getKVListByNamespace(
            final String namespace) throws RemotingException, ClientException, InterruptedException;

    /**
     * Delete topic in broker.
     *
     * @param addrs the addrs
     * @param topic the topic
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void deleteTopicInBroker(final Set<String> addrs, final String topic) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Delete topic in name server.
     *
     * @param addrs the addrs
     * @param topic the topic
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void deleteTopicInNameServer(final HashSet<String> addrs,
                                 final String topic) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Delete subscription group.
     *
     * @param addr      the addr
     * @param groupName the group name
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void deleteSubscriptionGroup(final String addr, String groupName) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Create and update kv config.
     *
     * @param namespace the namespace
     * @param key       the key
     * @param value     the value
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void createAndUpdateKvConfig(String namespace, String key,
                                 String value) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Delete kv config.
     *
     * @param namespace the namespace
     * @param key       the key
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void deleteKvConfig(String namespace, String key) throws RemotingException, BrokerException, InterruptedException,
            ClientException;

    /**
     * Reset offset by timestamp old list.
     *
     * @param consumerGroup the consumer group
     * @param topic         the topic
     * @param timestamp     the timestamp
     * @param force         the force
     * @return the list
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp, boolean force)
            throws RemotingException, BrokerException, InterruptedException, ClientException;

    /**
     * Reset offset by timestamp map.
     *
     * @param topic     the topic
     * @param group     the group
     * @param timestamp the timestamp
     * @param isForce   the is force
     * @return the map
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce)
            throws RemotingException, BrokerException, InterruptedException, ClientException;

    /**
     * Reset offset new.
     *
     * @param consumerGroup the consumer group
     * @param topic         the topic
     * @param timestamp     the timestamp
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void resetOffsetNew(String consumerGroup, String topic, long timestamp) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Gets consume status.
     *
     * @param topic      the topic
     * @param group      the group
     * @param clientAddr the client addr
     * @return the consume status
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group,
                                                          String clientAddr) throws RemotingException,
            BrokerException, InterruptedException, ClientException;

    /**
     * Create or update order conf.
     *
     * @param key       the key
     * @param value     the value
     * @param isCluster the is cluster
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void createOrUpdateOrderConf(String key, String value,
                                 boolean isCluster) throws RemotingException, BrokerException,
            InterruptedException, ClientException;

    /**
     * Query topic consume by who group list.
     *
     * @param topic the topic
     * @return the group list
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws InterruptedException         the interrupted exception
     * @throws BrokerException              the broker exception
     * @throws RemotingException            the remoting exception
     * @throws ClientException              the client exception
     */
    GroupList queryTopicConsumeByWho(final String topic) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, InterruptedException, BrokerException, RemotingException, ClientException;

    /**
     * Query consume time span list.
     *
     * @param topic the topic
     * @param group the group
     * @return the list
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     */
    List<QueueTimeSpan> queryConsumeTimeSpan(final String topic,
                                             final String group) throws InterruptedException, BrokerException,
            RemotingException, ClientException;

    /**
     * Clean expired consumer queue boolean.
     *
     * @param cluster the cluster
     * @return the boolean
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    boolean cleanExpiredConsumerQueue(String cluster) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, ClientException, InterruptedException;

    /**
     * Clean expired consumer queue by addr boolean.
     *
     * @param addr the addr
     * @return the boolean
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    boolean cleanExpiredConsumerQueueByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, ClientException, InterruptedException;

    /**
     * Clean unused topic boolean.
     *
     * @param cluster the cluster
     * @return the boolean
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    boolean cleanUnusedTopic(String cluster) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, ClientException, InterruptedException;

    /**
     * Clean unused topic by addr boolean.
     *
     * @param addr the addr
     * @return the boolean
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    boolean cleanUnusedTopicByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, ClientException, InterruptedException;

    /**
     * Gets consumer running info.
     *
     * @param consumerGroup the consumer group
     * @param clientId      the client id
     * @param jstack        the jstack
     * @return the consumer running info
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     */
    ConsumerRunningInfo getConsumerRunningInfo(final String consumerGroup, final String clientId, final boolean jstack)
            throws RemotingException, ClientException, InterruptedException;

    /**
     * Consume message directly consume message directly result.
     *
     * @param consumerGroup the consumer group
     * @param clientId      the client id
     * @param msgId         the msg id
     * @return the consume message directly result
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup,
                                                        String clientId,
                                                        String msgId) throws RemotingException, ClientException, InterruptedException, BrokerException;

    /**
     * Consume message directly consume message directly result.
     *
     * @param consumerGroup the consumer group
     * @param clientId      the client id
     * @param topic         the topic
     * @param msgId         the msg id
     * @return the consume message directly result
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup,
                                                        String clientId,
                                                        String topic,
                                                        String msgId) throws RemotingException, ClientException, InterruptedException, BrokerException;

    /**
     * Message track detail list.
     *
     * @param msg the msg
     * @return the list
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    List<MessageTrack> messageTrackDetail(
            ExtMessage msg) throws RemotingException, ClientException, InterruptedException,
            BrokerException;

    /**
     * Clone group offset.
     *
     * @param srcGroup  the src group
     * @param destGroup the dest group
     * @param topic     the topic
     * @param isOffline the is offline
     * @throws RemotingException    the remoting exception
     * @throws ClientException      the client exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline) throws RemotingException,
            ClientException, InterruptedException, BrokerException;

    /**
     * View broker stats data broker stats data.
     *
     * @param brokerAddr the broker addr
     * @param statsName  the stats name
     * @param statsKey   the stats key
     * @return the broker stats data
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    BrokerStatsData viewBrokerStatsData(final String brokerAddr, final String statsName, final String statsKey)
            throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, ClientException,
            InterruptedException;

    /**
     * Gets cluster list.
     *
     * @param topic the topic
     * @return the cluster list
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    Set<String> getClusterList(final String topic) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, ClientException, InterruptedException;

    /**
     * Fetch consume stats in broker consume stats list.
     *
     * @param brokerAddr    the broker addr
     * @param isOrder       the is order
     * @param timeoutMillis the timeout millis
     * @return the consume stats list
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws InterruptedException         the interrupted exception
     */
    ConsumeStatsList fetchConsumeStatsInBroker(final String brokerAddr, boolean isOrder,
                                               long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, ClientException, InterruptedException;

    /**
     * Gets topic cluster list.
     *
     * @param topic the topic
     * @return the topic cluster list
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     * @throws ClientException      the client exception
     * @throws RemotingException    the remoting exception
     */
    Set<String> getTopicClusterList(
            final String topic) throws InterruptedException, BrokerException, ClientException, RemotingException;

    /**
     * Gets all subscription group.
     *
     * @param brokerAddr    the broker addr
     * @param timeoutMillis the timeout millis
     * @return the all subscription group
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws BrokerException              the broker exception
     */
    SubscriptionGroupWrapper getAllSubscriptionGroup(final String brokerAddr,
                                                     long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
            RemotingConnectException, BrokerException;

    /**
     * Gets all topic group.
     *
     * @param brokerAddr    the broker addr
     * @param timeoutMillis the timeout millis
     * @return the all topic group
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws BrokerException              the broker exception
     */
    TopicConfigSerializeWrapper getAllTopicGroup(final String brokerAddr,
                                                 long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
            RemotingConnectException, BrokerException;

    /**
     * Update consume offset.
     *
     * @param brokerAddr   the broker addr
     * @param consumeGroup the consume group
     * @param mq           the mq
     * @param offset       the offset
     * @throws RemotingException    the remoting exception
     * @throws InterruptedException the interrupted exception
     * @throws BrokerException      the broker exception
     */
    void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq,
                             long offset) throws RemotingException, InterruptedException, BrokerException;

    /**
     * Update name server config.
     * <br>
     * Command Code : RequestCode.UPDATE_NAMESRV_CONFIG
     * <p>
     * <br> If param(nameServers) is null or empty, will use name servers from ns!
     *
     * @param properties  the properties
     * @param nameServers the name servers
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws ClientException              the client exception
     * @throws BrokerException              the broker exception
     */
    void updateNameServerConfig(final Properties properties,
                                final List<String> nameServers) throws InterruptedException, RemotingConnectException,
            UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException,
            ClientException, BrokerException;

    /**
     * Get name server config.
     * <br>
     * Command Code : RequestCode.GET_NAMESRV_CONFIG
     * <br> If param(nameServers) is null or empty, will use name servers from ns!
     *
     * @param nameServers the name servers
     * @return The fetched name server config
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws ClientException              the client exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    Map<String, Properties> getNameServerConfig(final List<String> nameServers) throws InterruptedException,
            RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException,
            ClientException, UnsupportedEncodingException;

    /**
     * query consume queue data
     *
     * @param brokerAddr    broker ip address
     * @param topic         topic
     * @param queueId       id of queue
     * @param index         start offset
     * @param count         how many
     * @param consumerGroup group
     * @return the query consume queue response body
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws ClientException              the client exception
     */
    QueryConsumeQueueResponseBody queryConsumeQueue(final String brokerAddr,
                                                    final String topic, final int queueId,
                                                    final long index, final int count, final String consumerGroup)
            throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, ClientException;

}