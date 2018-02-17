package cn.xianyijun.wisp.namesrv;

import cn.xianyijun.wisp.common.Configuration;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.common.namesrv.NameServerConfig;
import cn.xianyijun.wisp.namesrv.kvconfig.KVConfigManager;
import cn.xianyijun.wisp.namesrv.processor.ClusterRequestProcessor;
import cn.xianyijun.wisp.namesrv.processor.DefaultRequestProcessor;
import cn.xianyijun.wisp.namesrv.router.BrokerHousekeepingService;
import cn.xianyijun.wisp.namesrv.router.RouteInfoManager;
import cn.xianyijun.wisp.remoting.RemotingServer;
import cn.xianyijun.wisp.remoting.netty.NettyRemotingServer;
import cn.xianyijun.wisp.remoting.netty.NettyServerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class NameServerController {
    private static final String  CONFIG_STORE_PATH = "configStorePath";

    private final NameServerConfig nameServerConfig;

    private final NettyServerConfig nettyServerConfig;

    @Getter
    private Configuration configuration;

    private final KVConfigManager kvConfigManager;

    private final RouteInfoManager routeInfoManager;

    private BrokerHousekeepingService brokerHousekeepingService;

    private RemotingServer remotingServer;

    private ExecutorService remotingExecutor;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory(
            "NSScheduledThread"));

    public NameServerController(NameServerConfig nameServerConfig, NettyServerConfig nettyServerConfig) {
        this.nameServerConfig = nameServerConfig;
        this.nettyServerConfig = nettyServerConfig;
        this.kvConfigManager = new KVConfigManager(this);
        this.brokerHousekeepingService = new BrokerHousekeepingService(this);
        this.routeInfoManager = new RouteInfoManager();
        this.configuration = new Configuration(nameServerConfig, nettyServerConfig);
        this.configuration.setStorePathFromConfig(this.nameServerConfig, CONFIG_STORE_PATH);
    }

    public boolean initialize() {
        this.kvConfigManager.load();

        this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.brokerHousekeepingService);

        this.remotingExecutor =
                Executors.newFixedThreadPool(nettyServerConfig.getServerWorkerThreads(), new WispThreadFactory("RemotingExecutorThread_"));

        this.registerProcessor();

        this.scheduledExecutorService.scheduleAtFixedRate(NameServerController.this.routeInfoManager::scanNotActiveBroker, 5, 10, TimeUnit.SECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(NameServerController.this.kvConfigManager::printAllPeriodically, 1, 10, TimeUnit.MINUTES);

        return true;
    }

    private void registerProcessor() {
        if (nameServerConfig.isCluster()){
            this.remotingServer.registerDefaultProcessor(new ClusterRequestProcessor(this, nameServerConfig.getProductEnvName()),
                    this.remotingExecutor);
        }else {
            this.remotingServer.registerDefaultProcessor(new DefaultRequestProcessor(this), this.remotingExecutor);
        }
    }

    public void shutdown() {
        this.remotingServer.shutdown();
        this.remotingExecutor.shutdown();
        this.scheduledExecutorService.shutdown();
    }

    public void start() {
        this.remotingServer.start();
    }
}
