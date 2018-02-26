package cn.xianyijun.wisp.broker;

import cn.xianyijun.wisp.broker.client.ClientHousekeepingService;
import cn.xianyijun.wisp.broker.client.ConsumerIdsChangeListener;
import cn.xianyijun.wisp.broker.client.ConsumerManager;
import cn.xianyijun.wisp.broker.client.DefaultConsumerIdsChangeListener;
import cn.xianyijun.wisp.broker.client.ProducerManager;
import cn.xianyijun.wisp.broker.client.net.BrokerClient;
import cn.xianyijun.wisp.broker.filter.CommitLogDispatcherCalcBitMap;
import cn.xianyijun.wisp.broker.filter.ConsumerFilterManager;
import cn.xianyijun.wisp.broker.filterserver.FilterServerManager;
import cn.xianyijun.wisp.broker.latency.BrokerFixedThreadPoolExecutor;
import cn.xianyijun.wisp.broker.latency.FastFailureBroker;
import cn.xianyijun.wisp.broker.longpolling.NotifyMessageArrivingListener;
import cn.xianyijun.wisp.broker.longpolling.PullRequestHoldService;
import cn.xianyijun.wisp.broker.mqtrace.ConsumeMessageHook;
import cn.xianyijun.wisp.broker.mqtrace.ProduceMessageHook;
import cn.xianyijun.wisp.broker.offset.ConsumerOffsetManager;
import cn.xianyijun.wisp.broker.out.BrokerOuter;
import cn.xianyijun.wisp.broker.plugin.MessageStoreFactory;
import cn.xianyijun.wisp.broker.plugin.MessageStorePluginContext;
import cn.xianyijun.wisp.broker.processor.AdminBrokerProcessor;
import cn.xianyijun.wisp.broker.processor.ClientManageProcessor;
import cn.xianyijun.wisp.broker.processor.ConsumerManageProcessor;
import cn.xianyijun.wisp.broker.processor.EndTransactionProcessor;
import cn.xianyijun.wisp.broker.processor.ProduceMessageProcessor;
import cn.xianyijun.wisp.broker.processor.PullMessageProcessor;
import cn.xianyijun.wisp.broker.processor.QueryMessageProcessor;
import cn.xianyijun.wisp.broker.rebalance.ReBalanceLockManager;
import cn.xianyijun.wisp.broker.slave.SynchronizeSlave;
import cn.xianyijun.wisp.broker.subscription.SubscriptionGroupManager;
import cn.xianyijun.wisp.broker.topic.TopicConfigManager;
import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.common.Configuration;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.common.constant.PermName;
import cn.xianyijun.wisp.common.namesrv.RegisterBrokerResult;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.body.TopicConfigSerializeWrapper;
import cn.xianyijun.wisp.common.stats.MomentStatsItem;
import cn.xianyijun.wisp.remoting.RemotingServer;
import cn.xianyijun.wisp.remoting.netty.NettyClientConfig;
import cn.xianyijun.wisp.remoting.netty.NettyRemotingServer;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.netty.NettyServerConfig;
import cn.xianyijun.wisp.remoting.netty.RequestTask;
import cn.xianyijun.wisp.store.DefaultMessageStore;
import cn.xianyijun.wisp.store.MessageArrivingListener;
import cn.xianyijun.wisp.store.MessageStore;
import cn.xianyijun.wisp.store.config.BrokerRole;
import cn.xianyijun.wisp.store.config.MessageStoreConfig;
import cn.xianyijun.wisp.store.stats.BrokerStats;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
@Getter
@Setter
@Slf4j
public class BrokerController {
    private final BrokerConfig brokerConfig;
    private final NettyServerConfig nettyServerConfig;
    private final NettyClientConfig nettyClientConfig;
    private final MessageStoreConfig messageStoreConfig;

    private final ConsumerOffsetManager consumerOffsetManager;
    private final ConsumerManager consumerManager;
    private final ConsumerFilterManager consumerFilterManager;
    private final ProducerManager producerManager;
    private final ClientHousekeepingService clientHousekeepingService;
    private final PullMessageProcessor pullMessageProcessor;
    private final PullRequestHoldService pullRequestHoldService;
    private final MessageArrivingListener messageArrivingListener;
    private final BrokerClient brokerClient;
    private final SubscriptionGroupManager subscriptionGroupManager;
    private final ConsumerIdsChangeListener consumerIdsChangeListener;
    private final ReBalanceLockManager rebalanceLockManager = new ReBalanceLockManager();
    private final BrokerOuter brokerOuter;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory(
            "BrokerControllerScheduledThread"));
    private final SynchronizeSlave synchronizeSlave;
    private final BlockingQueue<Runnable> sendThreadPoolQueue;
    private final BlockingQueue<Runnable> pullThreadPoolQueue;
    private final BlockingQueue<Runnable> queryThreadPoolQueue;
    private final BlockingQueue<Runnable> clientManagerThreadPoolQueue;
    private final BlockingQueue<Runnable> consumerManagerThreadPoolQueue;
    private final FilterServerManager filterServerManager;
    private final BrokerStatsManager brokerStatsManager;
    private final List<ProduceMessageHook> produceMessageHookList = new ArrayList<>();
    private final List<ConsumeMessageHook> consumeMessageHookList = new ArrayList<ConsumeMessageHook>();
    private MessageStore messageStore;
    private RemotingServer remotingServer;
    private RemotingServer fastRemotingServer;
    private TopicConfigManager topicConfigManager;
    private ExecutorService produceMessageExecutor;
    private ExecutorService pullMessageExecutor;
    private ExecutorService queryMessageExecutor;
    private ExecutorService adminBrokerExecutor;
    private ExecutorService clientManageExecutor;
    private ExecutorService consumerManageExecutor;
    private boolean updateMasterHAServerAddrPeriodically = false;
    private BrokerStats brokerStats;
    private InetSocketAddress storeHost;
    private FastFailureBroker fastFailureBroker;

    private Configuration configuration;

    public BrokerController(
            final BrokerConfig brokerConfig,
            final NettyServerConfig nettyServerConfig,
            final NettyClientConfig nettyClientConfig,
            final MessageStoreConfig messageStoreConfig) {
        this.brokerConfig = brokerConfig;
        this.nettyServerConfig = nettyServerConfig;
        this.nettyClientConfig = nettyClientConfig;
        this.messageStoreConfig = messageStoreConfig;

        this.consumerOffsetManager = new ConsumerOffsetManager(this);
        this.topicConfigManager = new TopicConfigManager(this);
        this.pullMessageProcessor = new PullMessageProcessor(this);
        this.pullRequestHoldService = new PullRequestHoldService(this);
        this.messageArrivingListener = new NotifyMessageArrivingListener(this.pullRequestHoldService);
        this.consumerIdsChangeListener = new DefaultConsumerIdsChangeListener(this);
        this.consumerManager = new ConsumerManager(this.consumerIdsChangeListener);
        this.consumerFilterManager = new ConsumerFilterManager(this);
        this.producerManager = new ProducerManager();
        this.clientHousekeepingService = new ClientHousekeepingService(this);
        this.brokerClient = new BrokerClient(this);
        this.subscriptionGroupManager = new SubscriptionGroupManager(this);
        this.brokerOuter = new BrokerOuter(nettyClientConfig);
        this.filterServerManager = new FilterServerManager(this);

        this.synchronizeSlave = new SynchronizeSlave(this);

        this.sendThreadPoolQueue = new LinkedBlockingQueue<>(this.brokerConfig.getSendThreadPoolQueueCapacity());
        this.pullThreadPoolQueue = new LinkedBlockingQueue<>(this.brokerConfig.getPullThreadPoolQueueCapacity());
        this.queryThreadPoolQueue = new LinkedBlockingQueue<>(this.brokerConfig.getQueryThreadPoolQueueCapacity());
        this.clientManagerThreadPoolQueue = new LinkedBlockingQueue<>(this.brokerConfig.getClientManagerThreadPoolQueueCapacity());
        this.consumerManagerThreadPoolQueue = new LinkedBlockingQueue<>(this.brokerConfig.getConsumerManagerThreadPoolQueueCapacity());

        this.brokerStatsManager = new BrokerStatsManager(this.brokerConfig.getBrokerClusterName());
        this.setStoreHost(new InetSocketAddress(this.getBrokerConfig().getBrokerIP1(), this.getNettyServerConfig().getListenPort()));

        this.fastFailureBroker = new FastFailureBroker(this);

        this.configuration = new Configuration(
                BrokerPathConfigHelper.getBrokerConfigPath(),
                this.brokerConfig, this.nettyServerConfig, this.nettyClientConfig, this.messageStoreConfig
        );
    }

    public void shutdown() {


    }

    public boolean initialize() throws CloneNotSupportedException {
        boolean result = this.topicConfigManager.load();

        result = result && this.consumerOffsetManager.load();
        result = result && this.subscriptionGroupManager.load();
        result = result && this.consumerFilterManager.load();

        if (result) {
            try {
                this.messageStore =
                        new DefaultMessageStore(this.messageStoreConfig, this.brokerStatsManager, this.messageArrivingListener,
                                this.brokerConfig);
                this.brokerStats = new BrokerStats((DefaultMessageStore) this.messageStore);
                MessageStorePluginContext context = new MessageStorePluginContext(messageStoreConfig, brokerStatsManager, messageArrivingListener, brokerConfig);
                this.messageStore = MessageStoreFactory.build(context, this.messageStore);
                this.messageStore.getDispatcherList().addFirst(new CommitLogDispatcherCalcBitMap(this.brokerConfig, this.consumerFilterManager));
            } catch (IOException e) {
                result = false;
                log.error("Failed to initialize", e);
            }
        }

        result = result && this.messageStore.load();

        if (result) {
            this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.clientHousekeepingService);
            NettyServerConfig fastConfig = (NettyServerConfig) this.nettyServerConfig.clone();
            fastConfig.setListenPort(nettyServerConfig.getListenPort() - 2);
            this.fastRemotingServer = new NettyRemotingServer(fastConfig, this.clientHousekeepingService);

            this.produceMessageExecutor = new BrokerFixedThreadPoolExecutor(
                    this.brokerConfig.getSendMessageThreadPoolNums(),
                    this.brokerConfig.getSendMessageThreadPoolNums(),
                    1000 * 60,
                    TimeUnit.MILLISECONDS,
                    this.sendThreadPoolQueue,
                    new WispThreadFactory("ProduceMessageThread_"));

            this.queryMessageExecutor = new BrokerFixedThreadPoolExecutor(
                    this.brokerConfig.getQueryMessageThreadPoolNums(),
                    this.brokerConfig.getQueryMessageThreadPoolNums(),
                    1000 * 60,
                    TimeUnit.MILLISECONDS,
                    this.queryThreadPoolQueue,
                    new WispThreadFactory("QueryMessageThread_"));

            this.adminBrokerExecutor =
                    Executors.newFixedThreadPool(this.brokerConfig.getAdminBrokerThreadPoolNums(), new WispThreadFactory("AdminBrokerThread_"));

            this.clientManageExecutor = new ThreadPoolExecutor(
                    this.brokerConfig.getClientManageThreadPoolNums(),
                    this.brokerConfig.getClientManageThreadPoolNums(),
                    1000 * 60,
                    TimeUnit.MILLISECONDS,
                    this.clientManagerThreadPoolQueue,
                    new WispThreadFactory("ClientManageThread_"));

            this.consumerManageExecutor =
                    Executors.newFixedThreadPool(this.brokerConfig.getConsumerManageThreadPoolNums(), new WispThreadFactory(
                            "ConsumerManageThread_"));

            this.registerProcessor();

            final long initialDelay = UtilAll.computeNextMorningTimeMillis() - System.currentTimeMillis();
            final long period = 1000 * 60 * 60 * 24;
            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    BrokerController.this.getBrokerStats().record();
                } catch (Throwable e) {
                    log.error("schedule record error.", e);
                }
            }, initialDelay, period, TimeUnit.MILLISECONDS);

            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    BrokerController.this.consumerFilterManager.persist();
                } catch (Throwable e) {
                    log.error("schedule persist consumer filter error.", e);
                }
            }, 1000 * 10, 1000 * 10, TimeUnit.MILLISECONDS);

            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    BrokerController.this.protectBroker();
                } catch (Throwable e) {
                    log.error("protectBroker error.", e);
                }
            }, 3, 3, TimeUnit.MINUTES);

            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    BrokerController.this.printWaterMark();
                } catch (Throwable e) {
                    log.error("printWaterMark error.", e);
                }
            }, 10, 1, TimeUnit.SECONDS);

            this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    log.info("dispatch behind commit log {} bytes", BrokerController.this.getMessageStore().dispatchBehindBytes());
                } catch (Throwable e) {
                    log.error("schedule dispatchBehindBytes error.", e);
                }
            }, 1000 * 10, 1000 * 60, TimeUnit.MILLISECONDS);

            if (this.brokerConfig.getNameServerAddr() != null) {
                this.brokerOuter.updateNameServerAddressList(this.brokerConfig.getNameServerAddr());
                log.info("Set user specified name server address: {}", this.brokerConfig.getNameServerAddr());
            } else if (this.brokerConfig.isFetchNameServerAddrByAddressServer()) {
                this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        BrokerController.this.brokerOuter.fetchNameServerAddr();
                    } catch (Throwable e) {
                        log.error("ScheduledTask fetchNameServerAddr exception", e);
                    }
                }, 1000 * 10, 1000 * 60 * 2, TimeUnit.MILLISECONDS);
            }

            if (BrokerRole.SLAVE == this.messageStoreConfig.getBrokerRole()) {
                if (this.messageStoreConfig.getHaMasterAddress() != null && this.messageStoreConfig.getHaMasterAddress().length() >= 6) {
                    this.messageStore.updateHaMasterAddress(this.messageStoreConfig.getHaMasterAddress());
                    this.updateMasterHAServerAddrPeriodically = false;
                } else {
                    this.updateMasterHAServerAddrPeriodically = true;
                }

                this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        BrokerController.this.synchronizeSlave.syncAll();
                    } catch (Throwable e) {
                        log.error("ScheduledTask syncAll slave exception", e);
                    }
                }, 1000 * 10, 1000 * 60, TimeUnit.MILLISECONDS);
            } else {
                this.scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        BrokerController.this.printMasterAndSlaveDiff();
                    } catch (Throwable e) {
                        log.error("schedule printMasterAndSlaveDiff error.", e);
                    }
                }, 1000 * 10, 1000 * 60, TimeUnit.MILLISECONDS);
            }
        }
        return result;
    }

    private void registerProcessor() {
        ProduceMessageProcessor produceMessageProcessor = new ProduceMessageProcessor(this);
        produceMessageProcessor.setProduceMessageHookList(produceMessageHookList);
        produceMessageProcessor.setConsumeMessageHookList(consumeMessageHookList);


        this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE, produceMessageProcessor, this.produceMessageExecutor);
        this.remotingServer.registerProcessor(RequestCode.SEND_BATCH_MESSAGE, produceMessageProcessor, this.produceMessageExecutor);
        this.remotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, produceMessageProcessor, this.produceMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.SEND_MESSAGE, produceMessageProcessor, this.produceMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.SEND_BATCH_MESSAGE, produceMessageProcessor, this.produceMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, produceMessageProcessor, this.produceMessageExecutor);


        this.remotingServer.registerProcessor(RequestCode.PULL_MESSAGE, this.pullMessageProcessor, this.pullMessageExecutor);
        this.pullMessageProcessor.setConsumeMessageHookList(consumeMessageHookList);


        NettyRequestProcessor queryProcessor = new QueryMessageProcessor(this);
        this.remotingServer.registerProcessor(RequestCode.QUERY_MESSAGE, queryProcessor, this.queryMessageExecutor);
        this.remotingServer.registerProcessor(RequestCode.VIEW_MESSAGE_BY_ID, queryProcessor, this.queryMessageExecutor);

        this.fastRemotingServer.registerProcessor(RequestCode.QUERY_MESSAGE, queryProcessor, this.queryMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.VIEW_MESSAGE_BY_ID, queryProcessor, this.queryMessageExecutor);

        ClientManageProcessor clientProcessor = new ClientManageProcessor(this);
        this.remotingServer.registerProcessor(RequestCode.HEART_BEAT, clientProcessor, this.clientManageExecutor);
        this.remotingServer.registerProcessor(RequestCode.UNREGISTER_CLIENT, clientProcessor, this.clientManageExecutor);
        this.remotingServer.registerProcessor(RequestCode.CHECK_CLIENT_CONFIG, clientProcessor, this.clientManageExecutor);

        this.fastRemotingServer.registerProcessor(RequestCode.HEART_BEAT, clientProcessor, this.clientManageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.UNREGISTER_CLIENT, clientProcessor, this.clientManageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.CHECK_CLIENT_CONFIG, clientProcessor, this.clientManageExecutor);

        ConsumerManageProcessor consumerManageProcessor = new ConsumerManageProcessor(this);
        this.remotingServer.registerProcessor(RequestCode.GET_CONSUMER_LIST_BY_GROUP, consumerManageProcessor, this.consumerManageExecutor);
        this.remotingServer.registerProcessor(RequestCode.UPDATE_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);
        this.remotingServer.registerProcessor(RequestCode.QUERY_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);

        this.fastRemotingServer.registerProcessor(RequestCode.GET_CONSUMER_LIST_BY_GROUP, consumerManageProcessor, this.consumerManageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.UPDATE_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.QUERY_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);

        this.remotingServer.registerProcessor(RequestCode.END_TRANSACTION, new EndTransactionProcessor(this), this.produceMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.END_TRANSACTION, new EndTransactionProcessor(this), this.produceMessageExecutor);


        AdminBrokerProcessor adminProcessor = new AdminBrokerProcessor(this);
        this.remotingServer.registerDefaultProcessor(adminProcessor, this.adminBrokerExecutor);
        this.fastRemotingServer.registerDefaultProcessor(adminProcessor, this.adminBrokerExecutor);

    }

    public void start() throws Exception {
        if (this.messageStore != null) {
            this.messageStore.start();
        }

        if (this.remotingServer != null) {
            this.remotingServer.start();
        }

        if (this.fastRemotingServer != null) {
            this.fastRemotingServer.start();
        }

        if (this.brokerOuter != null) {
            this.brokerOuter.start();
        }

        if (this.pullRequestHoldService != null) {
            this.pullRequestHoldService.start();
        }

        if (this.clientHousekeepingService != null) {
            this.clientHousekeepingService.start();
        }

        if (this.filterServerManager != null) {
            this.filterServerManager.start();
        }

        this.registerBrokerAll(true, false);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                BrokerController.this.registerBrokerAll(true, false);
            } catch (Throwable e) {
                log.error("registerBrokerAll Exception", e);
            }
        }, 1000 * 10, 1000 * 30, TimeUnit.MILLISECONDS);

        if (this.brokerStatsManager != null) {
            this.brokerStatsManager.start();
        }

        if (this.fastFailureBroker != null) {
            this.fastFailureBroker.start();
        }
    }


    private void protectBroker() {
        if (this.brokerConfig.isDisableConsumeIfConsumerReadSlowly()) {
            for (Map.Entry<String, MomentStatsItem> next : this.brokerStatsManager.getMomentStatsItemSetFallSize().getStatsItemTable().entrySet()) {
                final long fallBehindBytes = next.getValue().getValue().get();
                if (fallBehindBytes > this.brokerConfig.getConsumerFallBehindThreshold()) {
                    final String[] split = next.getValue().getStatsKey().split("@");
                    final String group = split[2];
                    this.subscriptionGroupManager.disableConsume(group);
                }
            }
        }
    }

    public synchronized void registerBrokerAll(final boolean checkOrderConfig, boolean oneWay) {
        TopicConfigSerializeWrapper topicConfigWrapper = this.getTopicConfigManager().buildTopicConfigSerializeWrapper();

        if (!PermName.isWriteable(this.getBrokerConfig().getBrokerPermission())
                || !PermName.isReadable(this.getBrokerConfig().getBrokerPermission())) {
            ConcurrentHashMap<String, TopicConfig> topicConfigTable = new ConcurrentHashMap<String, TopicConfig>();
            for (TopicConfig topicConfig : topicConfigWrapper.getTopicConfigTable().values()) {
                TopicConfig tmp =
                        new TopicConfig(topicConfig.getTopicName(), topicConfig.getReadQueueNums(), topicConfig.getWriteQueueNums(),
                                this.brokerConfig.getBrokerPermission());
                topicConfigTable.put(topicConfig.getTopicName(), tmp);
            }
            topicConfigWrapper.setTopicConfigTable(topicConfigTable);
        }

        RegisterBrokerResult registerBrokerResult = this.brokerOuter.registerBrokerAll(
                this.brokerConfig.getBrokerClusterName(),
                this.getBrokerAddr(),
                this.brokerConfig.getBrokerName(),
                this.brokerConfig.getBrokerId(),
                this.getHAServerAddr(),
                topicConfigWrapper,
                this.filterServerManager.buildNewFilterServerList(),
                oneWay,
                this.brokerConfig.getRegisterBrokerTimeoutMills());

        if (registerBrokerResult != null) {
            if (this.updateMasterHAServerAddrPeriodically && registerBrokerResult.getHaServerAddr() != null) {
                this.messageStore.updateHaMasterAddress(registerBrokerResult.getHaServerAddr());
            }

            this.synchronizeSlave.setMasterAddr(registerBrokerResult.getMasterAddr());

            if (checkOrderConfig) {
                this.getTopicConfigManager().updateOrderTopicConfig(registerBrokerResult.getKvTable());
            }
        }
    }


    private long headSlowTimeMills(BlockingQueue<Runnable> q) {
        long slowTimeMills = 0;
        final Runnable peek = q.peek();
        if (peek != null) {
            RequestTask rt = FastFailureBroker.castRunnable(peek);
            slowTimeMills = rt == null ? 0 : this.messageStore.now() - rt.getCreateTimestamp();
        }

        if (slowTimeMills < 0) {
            slowTimeMills = 0;
        }

        return slowTimeMills;
    }

    public long headSlowTimeMills4SendThreadPoolQueue() {
        return this.headSlowTimeMills(this.sendThreadPoolQueue);
    }

    public long headSlowTimeMills4PullThreadPoolQueue() {
        return this.headSlowTimeMills(this.pullThreadPoolQueue);
    }

    public long headSlowTimeMills4QueryThreadPoolQueue() {
        return this.headSlowTimeMills(this.queryThreadPoolQueue);
    }


    private void printWaterMark() {
        log.info("[WATERMARK] Send Queue Size: {} SlowTimeMills: {}", this.sendThreadPoolQueue.size(), headSlowTimeMills4SendThreadPoolQueue());
        log.info("[WATERMARK] Pull Queue Size: {} SlowTimeMills: {}", this.pullThreadPoolQueue.size(), headSlowTimeMills4PullThreadPoolQueue());
        log.info("[WATERMARK] Query Queue Size: {} SlowTimeMills: {}", this.queryThreadPoolQueue.size(), headSlowTimeMills4QueryThreadPoolQueue());
    }

    private void printMasterAndSlaveDiff() {
        long diff = this.messageStore.slaveFallBehindMuch();
        log.info("Slave fall behind master: {} bytes", diff);
    }


    public String getBrokerAddr() {
        return this.brokerConfig.getBrokerIP1() + ":" + this.nettyServerConfig.getListenPort();
    }


    private String getHAServerAddr() {
        return this.brokerConfig.getBrokerIP2() + ":" + this.messageStoreConfig.getHaListenPort();
    }
}
