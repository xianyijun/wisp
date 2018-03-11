package cn.xianyijun.wisp.client.producer.factory;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.ClientRemotingProcessor;
import cn.xianyijun.wisp.client.MQClient;
import cn.xianyijun.wisp.client.MQClientManager;
import cn.xianyijun.wisp.client.admin.DefaultAdmin;
import cn.xianyijun.wisp.client.admin.MQAdminExtInner;
import cn.xianyijun.wisp.client.consumer.ConsumerInner;
import cn.xianyijun.wisp.client.consumer.ConsumerPullDelegate;
import cn.xianyijun.wisp.client.consumer.ConsumerPushDelegate;
import cn.xianyijun.wisp.client.consumer.FindBrokerResult;
import cn.xianyijun.wisp.client.consumer.ProcessQueue;
import cn.xianyijun.wisp.client.consumer.PullMessageService;
import cn.xianyijun.wisp.client.consumer.ReBalanceService;
import cn.xianyijun.wisp.client.producer.DefaultProducer;
import cn.xianyijun.wisp.client.producer.TopicPublishInfo;
import cn.xianyijun.wisp.client.producer.inner.MQProducerInner;
import cn.xianyijun.wisp.client.stat.ConsumerStatsManager;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.WispVersion;
import cn.xianyijun.wisp.common.constant.PermName;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumeMessageDirectlyResult;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumerData;
import cn.xianyijun.wisp.common.protocol.heartbeat.HeartbeatData;
import cn.xianyijun.wisp.common.protocol.heartbeat.ProducerData;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.common.protocol.route.BrokerData;
import cn.xianyijun.wisp.common.protocol.route.QueueData;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.filter.ExpressionType;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.remoting.netty.NettyClientConfig;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.utils.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.DatagramSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ClientInstance {

    private final static long LOCK_TIMEOUT_MILLIS = 3000;

    private final ClientConfig clientConfig;
    private final int instanceIndex;
    private final String clientId;
    private final NettyClientConfig nettyClientConfig;

    private final DefaultProducer defaultProducer;

    private final ClientRemotingProcessor clientRemotingProcessor;

    private final ConsumerStatsManager consumerStatsManager;

    private final MQClient client;

    private final DefaultAdmin admin;

    private final Lock lockHeartbeat = new ReentrantLock();

    private ServiceState serviceState = ServiceState.CREATE_JUST;

    private final ConcurrentMap<String, HashMap<Long, String>> brokerAddrTable =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, HashMap<String, Integer>> brokerVersionTable =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, MQProducerInner> producerTable = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, ConsumerInner> consumerTable = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, MQAdminExtInner> adminExtTable = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, TopicRouteData> topicRouteTable = new ConcurrentHashMap<>();

    private final AtomicLong sendHeartbeatTimesTotal = new AtomicLong(0);

    private final Lock nameServerLock = new ReentrantLock();

    private final PullMessageService pullMessageService;

    private final ReBalanceService reBalanceService;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MQClientFactoryScheduledThread"));

    private DatagramSocket datagramSocket;

    private Random random = new Random();

    public ClientInstance(ClientConfig clientConfig, int instanceIndex, String clientId, RPCHook rpcHook) {
        this.clientConfig = clientConfig;
        this.instanceIndex = instanceIndex;
        this.nettyClientConfig = new NettyClientConfig();
        this.nettyClientConfig.setClientCallbackExecutorThreads(clientConfig.getClientCallbackExecutorThreads());
        this.nettyClientConfig.setUseTLS(clientConfig.isUseTLS());
        this.clientRemotingProcessor = new ClientRemotingProcessor(this);
        this.pullMessageService = new PullMessageService(this);
        this.reBalanceService = new ReBalanceService(this);
        this.admin = new DefaultAdmin(this);
        this.client = new MQClient(this.nettyClientConfig, this.clientRemotingProcessor, rpcHook, clientConfig);
        if (this.clientConfig.getNameServerAddr() != null) {
            this.client.updateNameServerAddressList(this.clientConfig.getNameServerAddr());
            log.info("user specified name server address: {}", this.clientConfig.getNameServerAddr());
        }

        this.clientId = clientId;

        this.defaultProducer = new DefaultProducer(MixAll.CLIENT_INNER_PRODUCER_GROUP);
        this.defaultProducer.resetClientConfig(clientConfig);

        this.consumerStatsManager = new ConsumerStatsManager(this.scheduledExecutorService);

        log.info("created a new client Instance, FactoryIndex: {} ClinetID: {} {} {}, serializeType={}",
                this.instanceIndex,
                this.clientId,
                this.clientConfig,
                WispVersion.getVersionDesc(WispVersion.CURRENT_VERSION), RemotingCommand.getSerializeTypeConfigInThisServer());
    }


    public void sendHeartbeatToAllBrokerWithLock() {
        if (this.lockHeartbeat.tryLock()) {
            try {
                this.sendHeartbeatToAllBroker();
                this.uploadFilterClassSource();
            } catch (final Exception e) {
                log.error("sendHeartbeatToAllBroker exception", e);
            } finally {
                this.lockHeartbeat.unlock();
            }
        } else {
            log.warn("[sendHeartbeatToAllBrokerWithLock] lock heartBeat, but failed.");
        }
    }

    private void sendHeartbeatToAllBroker() {
        final HeartbeatData heartbeatData = this.prepareHeartbeatData();
        final boolean producerEmpty = heartbeatData.getProducerDataSet().isEmpty();
        final boolean consumerEmpty = heartbeatData.getConsumerDataSet().isEmpty();
        if (producerEmpty && consumerEmpty) {
            log.warn("sending heartbeat, but no consumer and no producer");
            return;
        }

        if (this.brokerAddrTable.isEmpty()) {
            return;
        }
        long times = this.sendHeartbeatTimesTotal.getAndIncrement();
        for (Map.Entry<String, HashMap<Long, String>> entry : this.brokerAddrTable.entrySet()) {
            String brokerName = entry.getKey();
            HashMap<Long, String> oneTable = entry.getValue();
            if (oneTable == null) {
                continue;
            }
            for (Map.Entry<Long, String> entry1 : oneTable.entrySet()) {
                Long id = entry1.getKey();
                String addr = entry1.getValue();
                if (StringUtils.isEmpty(addr)) {
                    continue;
                }
                if (consumerEmpty && id != MixAll.MASTER_ID) {
                    continue;
                }

                try {
                    int version = this.client.sendHearBeat(addr, heartbeatData, 3000);
                    if (!this.brokerVersionTable.containsKey(brokerName)) {
                        this.brokerVersionTable.put(brokerName, new HashMap<>(4));
                    }
                    this.brokerVersionTable.get(brokerName).put(addr, version);
                    if (times % 20 == 0) {
                        log.info("send heart beat to broker[{} {} {}] success", brokerName, id, addr);
                        log.info(heartbeatData.toString());
                    }
                } catch (Exception e) {
                    if (this.isBrokerInNameServer(addr)) {
                        log.info("send heart beat to broker[{} {} {}] failed", brokerName, id, addr);
                    } else {
                        log.info("send heart beat to broker[{} {} {}] exception, because the broker not up, forget it", brokerName,
                                id, addr);
                    }
                }
            }
        }
    }

    private HeartbeatData prepareHeartbeatData() {
        HeartbeatData heartbeatData = new HeartbeatData();

        // clientID
        heartbeatData.setClientID(this.clientId);

        // Consumer
        for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
            ConsumerInner impl = entry.getValue();
            if (impl != null) {
                ConsumerData consumerData = new ConsumerData();
                consumerData.setGroupName(impl.groupName());
                consumerData.setConsumeType(impl.consumeType());
                consumerData.setMessageModel(impl.messageModel());
                consumerData.setConsumeWhere(impl.consumeFromWhere());
                consumerData.getSubscriptionDataSet().addAll(impl.subscription());
                consumerData.setUnitMode(impl.isUnitMode());

                heartbeatData.getConsumerDataSet().add(consumerData);
            }
        }

        for (Map.Entry<String, MQProducerInner> entry : this.producerTable.entrySet()) {
            MQProducerInner impl = entry.getValue();
            if (impl != null) {
                ProducerData producerData = new ProducerData();
                producerData.setGroupName(entry.getKey());

                heartbeatData.getProducerDataSet().add(producerData);
            }
        }

        return heartbeatData;
    }

    private boolean isBrokerInNameServer(final String brokerAddr) {
        for (Map.Entry<String, TopicRouteData> itNext : this.topicRouteTable.entrySet()) {
            List<BrokerData> brokerDatas = itNext.getValue().getBrokerDatas();
            for (BrokerData bd : brokerDatas) {
                boolean contain = bd.getBrokerAddrs().containsValue(brokerAddr);
                if (contain) {
                    return true;
                }
            }
        }
        return false;
    }

    private void uploadFilterClassSource() {
        for (Map.Entry<String, ConsumerInner> next : this.consumerTable.entrySet()) {
            ConsumerInner consumer = next.getValue();
            if (ConsumeType.CONSUME_PASSIVELY == consumer.consumeType()) {
                Set<SubscriptionData> subscriptions = consumer.subscription();
                for (SubscriptionData sub : subscriptions) {
                    if (sub.isClassFilterMode() && sub.getFilterClassSource() != null) {
                        final String consumerGroup = consumer.groupName();
                        final String className = sub.getSubString();
                        final String topic = sub.getTopic();
                        final String filterClassSource = sub.getFilterClassSource();
                        try {
                            this.uploadFilterClassToAllFilterServer(consumerGroup, className, topic, filterClassSource);
                        } catch (Exception e) {
                            log.error("uploadFilterClassToAllFilterServer Exception", e);
                        }
                    }
                }
            }
        }
    }

    private void uploadFilterClassToAllFilterServer(final String consumerGroup, final String fullClassName,
                                                    final String topic,
                                                    final String filterClassSource) {
        byte[] classBody = null;
        int classCRC = 0;
        try {
            classBody = filterClassSource.getBytes(MixAll.DEFAULT_CHARSET);
            classCRC = UtilAll.crc32(classBody);
        } catch (Exception e1) {
            log.warn("uploadFilterClassToAllFilterServer Exception, ClassName: {} {}",
                    fullClassName,
                    RemotingHelper.exceptionSimpleDesc(e1));
        }

        TopicRouteData topicRouteData = this.topicRouteTable.get(topic);
        if (topicRouteData != null
                && topicRouteData.getFilterServerTable() != null && !topicRouteData.getFilterServerTable().isEmpty()) {
            for (Map.Entry<String, List<String>> next : topicRouteData.getFilterServerTable().entrySet()) {
                List<String> value = next.getValue();
                for (final String fsAddr : value) {
                    try {
                        this.client.registerMessageFilterClass(fsAddr, consumerGroup, topic, fullClassName, classCRC, classBody,
                                5000);

                        log.info("register message class filter to {} OK, ConsumerGroup: {} Topic: {} ClassName: {}", fsAddr, consumerGroup,
                                topic, fullClassName);

                    } catch (Exception e) {
                        log.error("uploadFilterClassToAllFilterServer Exception", e);
                    }
                }
            }
        } else {
            log.warn("register message class filter failed, because no filter server, ConsumerGroup: {} Topic: {} ClassName: {}",
                    consumerGroup, topic, fullClassName);
        }
    }

    public boolean registerProducer(final String group, final MQProducerInner producer) {
        if (null == group || null == producer) {
            return false;
        }

        MQProducerInner prev = this.producerTable.putIfAbsent(group, producer);
        if (prev != null) {
            log.warn("the producer group[{}] exist already.", group);
            return false;
        }
        return true;
    }

    public void start() throws ClientException {

        synchronized (this) {
            switch (this.serviceState) {
                case CREATE_JUST:
                    this.serviceState = ServiceState.START_FAILED;
                    if (null == this.clientConfig.getNameServerAddr()) {
                        this.client.fetchNameServerAddr();
                    }
                    this.client.start();
                    this.startScheduledTask();
                    this.pullMessageService.start();
                    this.reBalanceService.start();
                    this.defaultProducer.getProducerDelegate().start(false);

                    log.info("the client factory [{}] start OK", this.clientId);
                    this.serviceState = ServiceState.RUNNING;
                    break;
                case RUNNING:
                    break;
                case SHUTDOWN_ALREADY:
                    break;
                case START_FAILED:
                    throw new ClientException("The Factory object[" + this.getClientId() + "] has been created before, and failed.", null);
                default:
                    break;
            }
        }
    }


    private void startScheduledTask() {
        if (null == this.clientConfig.getNameServerAddr()) {
            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    ClientInstance.this.client.fetchNameServerAddr();
                } catch (Exception e) {
                    log.error("ScheduledTask fetchNameServerAddr exception", e);
                }
            }, 1000 * 10, 1000 * 60 * 2, TimeUnit.MILLISECONDS);
        }

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                ClientInstance.this.updateTopicRouteInfoFromNameServer();
            } catch (Exception e) {
                log.error("ScheduledTask updateTopicRouteInfoFromNameServer exception", e);
            }
        }, 10, this.clientConfig.getPollNameServerInterval(), TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                ClientInstance.this.cleanOfflineBroker();
                ClientInstance.this.sendHeartbeatToAllBrokerWithLock();
            } catch (Exception e) {
                log.error("ScheduledTask sendHeartbeatToAllBroker exception", e);
            }
        }, 1000, this.clientConfig.getHeartbeatBrokerInterval(), TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                ClientInstance.this.persistAllConsumerOffset();
            } catch (Exception e) {
                log.error("ScheduledTask persistAllConsumerOffset exception", e);
            }
        }, 1000 * 10, this.clientConfig.getPersistConsumerOffsetInterval(), TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                ClientInstance.this.adjustThreadPool();
            } catch (Exception e) {
                log.error("ScheduledTask adjustThreadPool exception", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }


    public void updateTopicRouteInfoFromNameServer() {
        Set<String> topicList = new HashSet<String>();

        // Consumer
        {
            for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
                ConsumerInner impl = entry.getValue();
                if (impl != null) {
                    Set<SubscriptionData> subList = impl.subscription();
                    if (subList != null) {
                        for (SubscriptionData subData : subList) {
                            topicList.add(subData.getTopic());
                        }
                    }
                }
            }
        }

        // Producer
        {
            for (Map.Entry<String, MQProducerInner> entry : this.producerTable.entrySet()) {
                MQProducerInner impl = entry.getValue();
                if (impl != null) {
                    Set<String> lst = impl.getPublishTopicList();
                    topicList.addAll(lst);
                }
            }
        }

        for (String topic : topicList) {
            this.updateTopicRouteInfoFromNameServer(topic);
        }
    }

    public boolean updateTopicRouteInfoFromNameServer(final String topic) {
        return updateTopicRouteInfoFromNameServer(topic, false, null);
    }

    public boolean updateTopicRouteInfoFromNameServer(final String topic, boolean isDefault,
                                                       DefaultProducer defaultProducer) {
        try {
            if (this.nameServerLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    TopicRouteData topicRouteData;
                    if (isDefault && defaultProducer != null) {
                        topicRouteData = this.client.getDefaultTopicRouteInfoFromNameServer(defaultProducer.getCreateTopicKey(),
                                1000 * 3);
                        if (topicRouteData != null) {
                            for (QueueData data : topicRouteData.getQueueDatas()) {
                                int queueNums = Math.min(defaultProducer.getDefaultTopicQueueNums(), data.getReadQueueNums());
                                data.setReadQueueNums(queueNums);
                                data.setWriteQueueNums(queueNums);
                            }
                        }
                    } else {
                        topicRouteData = this.client.getTopicRouteInfoFromNameServer(topic, 1000 * 3);
                    }
                    if (topicRouteData != null) {
                        TopicRouteData old = this.topicRouteTable.get(topic);
                        boolean changed = topicRouteDataIsChange(old, topicRouteData);
                        if (!changed) {
                            changed = this.isNeedUpdateTopicRouteInfo(topic);
                        } else {
                            log.info("the topic[{}] route info changed, old[{}] ,new[{}]", topic, old, topicRouteData);
                        }

                        if (changed) {
                            TopicRouteData cloneTopicRouteData = topicRouteData.cloneTopicRouteData();

                            for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                                this.brokerAddrTable.put(bd.getBrokerName(), bd.getBrokerAddrs());
                            }

                            {
                                TopicPublishInfo publishInfo = topicRouteData2TopicPublishInfo(topic, topicRouteData);
                                publishInfo.setHaveTopicRouterInfo(true);
                                for (Map.Entry<String, MQProducerInner> entry : this.producerTable.entrySet()) {
                                    MQProducerInner impl = entry.getValue();
                                    if (impl != null) {
                                        impl.updateTopicPublishInfo(topic, publishInfo);
                                    }
                                }
                            }

                            // Update sub info
                            {
                                Set<MessageQueue> subscribeInfo = topicRouteData2TopicSubscribeInfo(topic, topicRouteData);
                                for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
                                    ConsumerInner impl = entry.getValue();
                                    if (impl != null) {
                                        impl.updateTopicSubscribeInfo(topic, subscribeInfo);
                                    }
                                }
                            }
                            log.info("topicRouteTable.put. Topic = {}, TopicRouteData[{}]", topic, cloneTopicRouteData);
                            this.topicRouteTable.put(topic, cloneTopicRouteData);
                            return true;
                        }
                    } else {
                        log.warn("updateTopicRouteInfoFromNameServer, getTopicRouteInfoFromNameServer return null, Topic: {}", topic);
                    }
                } catch (Exception e) {
                    if (!topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) && !topic.equals(MixAll.DEFAULT_TOPIC)) {
                        log.warn("updateTopicRouteInfoFromNameServer Exception", e);
                    }
                } finally {
                    this.nameServerLock.unlock();
                }
            } else {
                log.warn("updateTopicRouteInfoFromNameServer tryLock timeout {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            log.warn("updateTopicRouteInfoFromNameServer Exception", e);
        }

        return false;
    }


    private boolean isBrokerAddrExistInTopicRouteTable(final String addr) {
        for (Map.Entry<String, TopicRouteData> entry : this.topicRouteTable.entrySet()) {
            TopicRouteData topicRouteData = entry.getValue();
            List<BrokerData> bds = topicRouteData.getBrokerDatas();
            for (BrokerData bd : bds) {
                if (bd.getBrokerAddrs() != null) {
                    boolean exist = bd.getBrokerAddrs().containsValue(addr);
                    if (exist) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private boolean topicRouteDataIsChange(TopicRouteData oldData, TopicRouteData nowData) {
        if (oldData == null || nowData == null) {
            return true;
        }
        TopicRouteData old = oldData.cloneTopicRouteData();
        TopicRouteData now = nowData.cloneTopicRouteData();
        Collections.sort(old.getQueueDatas());
        Collections.sort(old.getBrokerDatas());
        Collections.sort(now.getQueueDatas());
        Collections.sort(now.getBrokerDatas());
        return !old.equals(now);
    }

    private boolean isNeedUpdateTopicRouteInfo(final String topic) {
        boolean result = false;
        {
            Iterator<Map.Entry<String, MQProducerInner>> it = this.producerTable.entrySet().iterator();
            while (it.hasNext() && !result) {
                Map.Entry<String, MQProducerInner> entry = it.next();
                MQProducerInner impl = entry.getValue();
                if (impl != null) {
                    result = impl.isPublishTopicNeedUpdate(topic);
                }
            }
        }

        {
            Iterator<Map.Entry<String, ConsumerInner>> it = this.consumerTable.entrySet().iterator();
            while (it.hasNext() && !result) {
                Map.Entry<String, ConsumerInner> entry = it.next();
                ConsumerInner impl = entry.getValue();
                if (impl != null) {
                    result = impl.isSubscribeTopicNeedUpdate(topic);
                }
            }
        }

        return result;
    }


    public static TopicPublishInfo topicRouteData2TopicPublishInfo(final String topic, final TopicRouteData route) {
        TopicPublishInfo info = new TopicPublishInfo();
        info.setTopicRouteData(route);
        if (route.getOrderTopicConf() != null && route.getOrderTopicConf().length() > 0) {
            String[] brokers = route.getOrderTopicConf().split(";");
            for (String broker : brokers) {
                String[] item = broker.split(":");
                int nums = Integer.parseInt(item[1]);
                for (int i = 0; i < nums; i++) {
                    MessageQueue mq = new MessageQueue(topic, item[0], i);
                    info.getMessageQueueList().add(mq);
                }
            }

            info.setOrderTopic(true);
        } else {
            List<QueueData> qds = route.getQueueDatas();
            Collections.sort(qds);
            for (QueueData qd : qds) {
                if (PermName.isWriteable(qd.getPerm())) {
                    BrokerData brokerData = null;
                    for (BrokerData bd : route.getBrokerDatas()) {
                        if (bd.getBrokerName().equals(qd.getBrokerName())) {
                            brokerData = bd;
                            break;
                        }
                    }

                    if (null == brokerData) {
                        continue;
                    }

                    if (!brokerData.getBrokerAddrs().containsKey(MixAll.MASTER_ID)) {
                        continue;
                    }

                    for (int i = 0; i < qd.getWriteQueueNums(); i++) {
                        MessageQueue mq = new MessageQueue(topic, qd.getBrokerName(), i);
                        info.getMessageQueueList().add(mq);
                    }
                }
            }

            info.setOrderTopic(false);
        }

        return info;
    }


    private static Set<MessageQueue> topicRouteData2TopicSubscribeInfo(final String topic, final TopicRouteData route) {
        Set<MessageQueue> mqList = new HashSet<MessageQueue>();
        List<QueueData> qds = route.getQueueDatas();
        for (QueueData qd : qds) {
            if (PermName.isReadable(qd.getPerm())) {
                for (int i = 0; i < qd.getReadQueueNums(); i++) {
                    MessageQueue mq = new MessageQueue(topic, qd.getBrokerName(), i);
                    mqList.add(mq);
                }
            }
        }
        return mqList;
    }

    private void cleanOfflineBroker() {
        try {
            if (this.nameServerLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    ConcurrentHashMap<String, HashMap<Long, String>> updatedTable = new ConcurrentHashMap<>();

                    Iterator<Map.Entry<String, HashMap<Long, String>>> itBrokerTable = this.brokerAddrTable.entrySet().iterator();
                    while (itBrokerTable.hasNext()) {
                        Map.Entry<String, HashMap<Long, String>> entry = itBrokerTable.next();
                        String brokerName = entry.getKey();
                        HashMap<Long, String> oneTable = entry.getValue();

                        HashMap<Long, String> cloneAddrTable = new HashMap<>(oneTable);

                        Iterator<Map.Entry<Long, String>> it = cloneAddrTable.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<Long, String> ee = it.next();
                            String addr = ee.getValue();
                            if (!this.isBrokerAddrExistInTopicRouteTable(addr)) {
                                it.remove();
                                log.info("the broker addr[{} {}] is offline, remove it", brokerName, addr);
                            }
                        }

                        if (cloneAddrTable.isEmpty()) {
                            itBrokerTable.remove();
                            log.info("the broker[{}] name's host is offline, remove it", brokerName);
                        } else {
                            updatedTable.put(brokerName, cloneAddrTable);
                        }
                    }

                    if (!updatedTable.isEmpty()) {
                        this.brokerAddrTable.putAll(updatedTable);
                    }
                } finally {
                    this.nameServerLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.warn("cleanOfflineBroker Exception", e);
        }
    }


    private void persistAllConsumerOffset() {
        for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
            ConsumerInner impl = entry.getValue();
            impl.persistConsumerOffset();
        }
    }

    private void adjustThreadPool() {
        for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
            ConsumerInner impl = entry.getValue();
            if (impl != null) {
                try {
                    if (impl instanceof ConsumerPushDelegate) {
                        ConsumerPushDelegate consumerPushDelegate = (ConsumerPushDelegate) impl;
                        consumerPushDelegate.adjustThreadPool();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void unregisterProducer(final String group) {
        this.producerTable.remove(group);
        this.unregisterClientWithLock(group, null);
    }

    private void unregisterClientWithLock(final String producerGroup, final String consumerGroup) {
        try {
            if (this.lockHeartbeat.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    this.unregisterClient(producerGroup, consumerGroup);
                } catch (Exception e) {
                    log.error("unregisterClient exception", e);
                } finally {
                    this.lockHeartbeat.unlock();
                }
            } else {
                log.warn("[unregisterClientWithLock] lock heartBeat, but failed.");
            }
        } catch (InterruptedException e) {
            log.warn("unregisterClientWithLock exception", e);
        }
    }

    private void unregisterClient(final String producerGroup, final String consumerGroup) {
        for (Map.Entry<String, HashMap<Long, String>> entry : this.brokerAddrTable.entrySet()) {
            String brokerName = entry.getKey();
            HashMap<Long, String> oneTable = entry.getValue();

            if (oneTable != null) {
                for (Map.Entry<Long, String> entry1 : oneTable.entrySet()) {
                    String addr = entry1.getValue();
                    if (addr != null) {
                        try {
                            this.client.unregisterClient(addr, this.clientId, producerGroup, consumerGroup, 3000);
                            log.info("unregister client[Producer: {} Consumer: {}] from broker[{} {} {}] success", producerGroup, consumerGroup, brokerName, entry1.getKey(), addr);
                        } catch (RemotingException | BrokerException | InterruptedException e) {
                            log.error("unregister client exception from broker: " + addr, e);
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        // Consumer
        if (!this.consumerTable.isEmpty()) {
            return;
        }

        // AdminExt
        if (!this.adminExtTable.isEmpty()) {
            return;
        }

        // Producer
        if (this.producerTable.size() > 1) {
            return;
        }

        synchronized (this) {
            switch (this.serviceState) {
                case CREATE_JUST:
                    break;
                case RUNNING:
                    this.defaultProducer.getProducerDelegate().shutdown(false);

                    this.serviceState = ServiceState.SHUTDOWN_ALREADY;
                    this.pullMessageService.shutdown(true);
                    this.scheduledExecutorService.shutdown();
                    this.client.shutdown();
                    this.reBalanceService.shutdown();

                    if (this.datagramSocket != null) {
                        this.datagramSocket.close();
                        this.datagramSocket = null;
                    }
                    MQClientManager.getInstance().removeClientFactory(this.clientId);
                    log.info("the client factory [{}] shutdown OK", this.clientId);
                    break;
                case SHUTDOWN_ALREADY:
                    break;
                default:
                    break;
            }
        }
    }


    public String findBrokerAddressInPublish(final String brokerName) {
        HashMap<Long/* brokerId */, String/* address */> map = this.brokerAddrTable.get(brokerName);
        if (map != null && !map.isEmpty()) {
            return map.get(MixAll.MASTER_ID);
        }
        return null;
    }

    public MQProducerInner selectProducer(final String group) {
        return this.producerTable.get(group);
    }


    public void reBalanceImmediately() {
        this.reBalanceService.wakeup();
    }

    public void resetOffset(String topic, String group, Map<MessageQueue, Long> offsetTable) {
        ConsumerPushDelegate consumer = null;
        try {
            ConsumerInner impl = this.consumerTable.get(group);
            if (impl != null && impl instanceof ConsumerPushDelegate) {
                consumer = (ConsumerPushDelegate) impl;
            } else {
                log.info("[reset-offset] consumer dose not exist. group={}", group);
                return;
            }
            consumer.suspend();

            ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = consumer.getReBalance().getProcessQueueTable();
            for (Map.Entry<MessageQueue, ProcessQueue> entry : processQueueTable.entrySet()) {
                MessageQueue messageQueue = entry.getKey();
                if (topic.equals(messageQueue.getTopic()) && offsetTable.containsKey(messageQueue)) {
                    ProcessQueue processQueue = entry.getValue();
                    processQueue.setDropped(true);
                    processQueue.clear();
                }
            }

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ignored) {
            }

            Iterator<MessageQueue> iterator = processQueueTable.keySet().iterator();
            while (iterator.hasNext()) {
                MessageQueue mq = iterator.next();
                Long offset = offsetTable.get(mq);
                if (topic.equals(mq.getTopic()) && offset != null) {
                    try {
                        consumer.updateConsumeOffset(mq, offset);
                        consumer.getReBalance().removeUnnecessaryMessageQueue(mq, processQueueTable.get(mq));
                        iterator.remove();
                    } catch (Exception e) {
                        log.warn("reset offset failed. group={}, {}", group, mq, e);
                    }
                }
            }
        } finally {
            if (consumer != null) {
                consumer.resume();
            }
        }
    }


    public boolean registerConsumer(final String group, final ConsumerInner consumer) {
        if (null == group || null == consumer) {
            return false;
        }

        ConsumerInner prev = this.consumerTable.putIfAbsent(group, consumer);
        if (prev != null) {
            log.warn("the consumer group[" + group + "] exist already.");
            return false;
        }
        return true;
    }

    public void checkClientInBroker() throws ClientException {

        for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
            Set<SubscriptionData> subscriptionInner = entry.getValue().subscription();
            if (subscriptionInner == null || subscriptionInner.isEmpty()) {
                return;
            }

            for (SubscriptionData subscriptionData : subscriptionInner) {
                if (ExpressionType.isTagType(subscriptionData.getExpressionType())) {
                    continue;
                }
                String addr = findBrokerAddrByTopic(subscriptionData.getTopic());

                if (addr != null) {
                    try {
                        this.client.checkClientInBroker(
                                addr, entry.getKey(), this.clientId, subscriptionData, 3 * 1000
                        );
                    } catch (Exception e) {
                        if (e instanceof ClientException) {
                            throw (ClientException) e;
                        } else {
                            throw new ClientException("Check client in broker error, maybe because you use "
                                    + subscriptionData.getExpressionType() + " to filter message, but server has not been upgraded to support!"
                                    + "This error would not affect the launch of consumer, but may has impact on message receiving if you " +
                                    "have use the new features which are not supported by server, please check the log!", e);
                        }
                    }
                }
            }
        }
    }

    private String findBrokerAddrByTopic(final String topic) {
        TopicRouteData topicRouteData = this.topicRouteTable.get(topic);
        if (topicRouteData != null) {
            List<BrokerData> brokers = topicRouteData.getBrokerDatas();
            if (!brokers.isEmpty()) {
                int index = random.nextInt(brokers.size());
                BrokerData bd = brokers.get(index % brokers.size());
                return bd.selectBrokerAddr();
            }
        }

        return null;
    }

    public Map<MessageQueue, Long> getConsumerStatus(String topic, String group) {
        ConsumerInner impl = this.consumerTable.get(group);
        if (impl != null && impl instanceof ConsumerPushDelegate) {
            ConsumerPushDelegate consumer = (ConsumerPushDelegate) impl;
            return consumer.getOffsetStore().cloneOffsetTable(topic);
        } else if (impl != null && impl instanceof ConsumerPullDelegate) {
            ConsumerPullDelegate consumer = (ConsumerPullDelegate) impl;
            return consumer.getOffsetStore().cloneOffsetTable(topic);
        } else {
            return Collections.emptyMap();
        }
    }

    public ConsumerRunningInfo consumerRunningInfo(final String consumerGroup) {
        ConsumerInner mqConsumerInner = this.consumerTable.get(consumerGroup);

        ConsumerRunningInfo consumerRunningInfo = mqConsumerInner.consumerRunningInfo();

        List<String> nsList = this.client.getRemotingClient().getNameServerAddressList();

        StringBuilder strBuilder = new StringBuilder();
        if (nsList != null) {
            for (String addr : nsList) {
                strBuilder.append(addr).append(";");
            }
        }

        String nsAddr = strBuilder.toString();
        consumerRunningInfo.getProperties().put(ConsumerRunningInfo.PROP_NAME_SERVER_ADDR, nsAddr);
        consumerRunningInfo.getProperties().put(ConsumerRunningInfo.PROP_CONSUME_TYPE, mqConsumerInner.consumeType().name());
        consumerRunningInfo.getProperties().put(ConsumerRunningInfo.PROP_CLIENT_VERSION,
                WispVersion.getVersionDesc(WispVersion.CURRENT_VERSION));

        return consumerRunningInfo;
    }


    public ConsumeMessageDirectlyResult consumeMessageDirectly(final ExtMessage msg,
                                                               final String consumerGroup,
                                                               final String brokerName) {
        ConsumerInner consumerInner = this.consumerTable.get(consumerGroup);
        if (null != consumerInner) {
            ConsumerPushDelegate consumer = (ConsumerPushDelegate) consumerInner;

            return consumer.getConsumeMessageService().consumeMessageDirectly(msg, brokerName);
        }

        return null;
    }

    public void doReBalance() {
        for (Map.Entry<String, ConsumerInner> entry : this.consumerTable.entrySet()) {
            ConsumerInner impl = entry.getValue();
            if (impl != null) {
                try {
                    impl.doReBalance();
                } catch (Throwable e) {
                    log.error("doReBalance exception", e);
                }
            }
        }
    }


    public List<String> findConsumerIdList(final String topic, final String group) {
        String brokerAddr = this.findBrokerAddrByTopic(topic);
        if (null == brokerAddr) {
            this.updateTopicRouteInfoFromNameServer(topic);
            brokerAddr = this.findBrokerAddrByTopic(topic);
        }

        if (null != brokerAddr) {
            try {
                return this.client.getConsumerIdListByGroup(brokerAddr, group, 3000);
            } catch (Exception e) {
                log.warn("getConsumerIdListByGroup exception, " + brokerAddr + " " + group, e);
            }
        }

        return null;
    }

    public FindBrokerResult findBrokerAddressInSubscribe(
            final String brokerName,
            final long brokerId,
            final boolean onlyThisBroker
    ) {
        String brokerAddr = null;
        boolean slave = false;
        boolean found = false;

        HashMap<Long/* brokerId */, String/* address */> map = this.brokerAddrTable.get(brokerName);
        if (map != null && !map.isEmpty()) {
            brokerAddr = map.get(brokerId);
            slave = brokerId != MixAll.MASTER_ID;
            found = brokerAddr != null;

            if (!found && !onlyThisBroker) {
                Map.Entry<Long, String> entry = map.entrySet().iterator().next();
                brokerAddr = entry.getValue();
                slave = entry.getKey() != MixAll.MASTER_ID;
                found = true;
            }
        }

        if (found) {
            return new FindBrokerResult(brokerAddr, slave, findBrokerVersion(brokerName, brokerAddr));
        }

        return null;
    }

    public int findBrokerVersion(String brokerName, String brokerAddr) {
        if (this.brokerVersionTable.containsKey(brokerName)) {
            if (this.brokerVersionTable.get(brokerName).containsKey(brokerAddr)) {
                return this.brokerVersionTable.get(brokerName).get(brokerAddr);
            }
        }
        return 0;
    }

    public FindBrokerResult findBrokerAddressInAdmin(final String brokerName) {
        String brokerAddr = null;
        boolean slave = false;
        boolean found = false;

        HashMap<Long/* brokerId */, String/* address */> map = this.brokerAddrTable.get(brokerName);
        if (map != null && !map.isEmpty()) {
            for (Map.Entry<Long, String> entry : map.entrySet()) {
                Long id = entry.getKey();
                brokerAddr = entry.getValue();
                if (brokerAddr != null) {
                    found = true;
                    slave = MixAll.MASTER_ID != id;
                    break;

                }
            } // end of for
        }

        if (found) {
            return new FindBrokerResult(brokerAddr, slave, findBrokerVersion(brokerName, brokerAddr));
        }
        return null;
    }

    public TopicRouteData getAnExistTopicRouteData(final String topic) {
        return this.topicRouteTable.get(topic);
    }

    public ConsumerInner selectConsumer(final String group) {
        return this.consumerTable.get(group);
    }
}
