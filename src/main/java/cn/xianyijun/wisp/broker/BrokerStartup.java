package cn.xianyijun.wisp.broker;

import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.TlsMode;
import cn.xianyijun.wisp.common.WispVersion;
import cn.xianyijun.wisp.remoting.netty.NettyClientConfig;
import cn.xianyijun.wisp.remoting.netty.NettyServerConfig;
import cn.xianyijun.wisp.remoting.netty.NettySystemConfig;
import cn.xianyijun.wisp.remoting.netty.TlsSystemConfig;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.store.config.MessageStoreConfig;
import cn.xianyijun.wisp.utils.RemotingUtils;
import cn.xianyijun.wisp.utils.ServerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.xianyijun.wisp.remoting.netty.TlsSystemConfig.TLS_ENABLE;
import static cn.xianyijun.wisp.store.config.BrokerRole.SLAVE;
import static cn.xianyijun.wisp.utils.ServerUtils.buildCommandlineOptions;

/**
 * @author xianyijun
 */
@Slf4j
public class BrokerStartup {
    public static Properties properties = null;
    public static String configFile = null;

    public static void main(String[] args) {
        start(createBrokerController(args));
    }

    private static BrokerController createBrokerController(String[] args) {
        System.setProperty(MixAll.WISP_HOME_PROPERTY, "/Users/xianyijun/code/git/wisp/");
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(WispVersion.CURRENT_VERSION));

        if (null == System.getProperty(NettySystemConfig.WISP_REMOTING_SOCKET_SNDBUF_SIZE)) {
            NettySystemConfig.socketSndBufSize = 131072;
        }

        if (null == System.getProperty(NettySystemConfig.WISP_REMOTING_SOCKET_RCVBUF_SIZE)) {
            NettySystemConfig.socketRcvBufSize = 131072;
        }

        try {
            Options options = ServerUtils.buildCommandlineOptions(new Options());
            CommandLine commandLine = ServerUtils.parseCmdLine("broker", args, buildCommandlineOptions(options),
                    new PosixParser());
            if (null == commandLine) {
                System.exit(-1);
            }

            final BrokerConfig brokerConfig = new BrokerConfig();
            final NettyServerConfig nettyServerConfig = new NettyServerConfig();
            final NettyClientConfig nettyClientConfig = new NettyClientConfig();

            nettyClientConfig.setUseTLS(Boolean.parseBoolean(System.getProperty(TLS_ENABLE,
                    String.valueOf(TlsSystemConfig.tlsMode == TlsMode.ENFORCING))));
            nettyServerConfig.setListenPort(10911);
            final MessageStoreConfig messageStoreConfig = new MessageStoreConfig();

            if (SLAVE == messageStoreConfig.getBrokerRole()) {
                int ratio = messageStoreConfig.getAccessMessageInMemoryMaxRatio() - 10;
                messageStoreConfig.setAccessMessageInMemoryMaxRatio(ratio);
            }

            if (commandLine.hasOption('c')) {
                String file = commandLine.getOptionValue('c');
                if (file != null) {
                    configFile = file;
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties = new Properties();
                    properties.load(in);

                    properties2SystemEnv(properties);
                    MixAll.properties2Object(properties, brokerConfig);
                    MixAll.properties2Object(properties, nettyServerConfig);
                    MixAll.properties2Object(properties, nettyClientConfig);
                    MixAll.properties2Object(properties, messageStoreConfig);

                    BrokerPathConfigHelper.setBrokerConfigPath(file);
                    in.close();
                }
            }

            MixAll.properties2Object(ServerUtils.commandLine2Properties(commandLine), brokerConfig);

            if (null == brokerConfig.getWispHome()) {
                log.info("Please set the {} variable in your environment to match the location of the RocketMQ installation", MixAll.WISP_HOME_ENV);
                System.exit(-2);
            }

            String nameServerAddr = brokerConfig.getNameServerAddr();
            if (null != nameServerAddr) {
                try {
                    String[] addrArray = nameServerAddr.split(";");
                    for (String addr : addrArray) {
                        RemotingUtils.string2SocketAddress(addr);
                    }
                } catch (Exception e) {
                    System.out.printf(
                            "The Name Server Address[%s] illegal, please set it as follows, \"127.0.0.1:9876;192.168.0.1:9876\"%n",
                            nameServerAddr);
                    System.exit(-3);
                }
            }

            switch (messageStoreConfig.getBrokerRole()) {
                case ASYNC_MASTER:
                case SYNC_MASTER:
                    brokerConfig.setBrokerId(MixAll.MASTER_ID);
                    break;
                case SLAVE:
                    if (brokerConfig.getBrokerId() <= 0) {
                        log.info("Slave's brokerId must be > 0");
                        System.exit(-3);
                    }
                    break;
                default:
                    break;
            }

            messageStoreConfig.setHaListenPort(nettyServerConfig.getListenPort() + 1);

            if (commandLine.hasOption('p')) {
                MixAll.printObjectProperties(brokerConfig);
                MixAll.printObjectProperties(nettyServerConfig);
                MixAll.printObjectProperties(nettyClientConfig);
                MixAll.printObjectProperties(messageStoreConfig);
                System.exit(0);
            } else if (commandLine.hasOption('m')) {
                MixAll.printObjectProperties(brokerConfig, true);
                MixAll.printObjectProperties(nettyServerConfig, true);
                MixAll.printObjectProperties(nettyClientConfig, true);
                MixAll.printObjectProperties(messageStoreConfig, true);
                System.exit(0);
            }

            MixAll.printObjectProperties(brokerConfig);
            MixAll.printObjectProperties(nettyServerConfig);
            MixAll.printObjectProperties(nettyClientConfig);
            MixAll.printObjectProperties(messageStoreConfig);

            final BrokerController controller = new BrokerController(
                    brokerConfig,
                    nettyServerConfig,
                    nettyClientConfig,
                    messageStoreConfig);
            controller.getConfiguration().registerConfig(properties);

            boolean initResult = controller.initialize();
            log.info("[BrokerStartUp] doMain initialize result :{}  ", initResult);

            if (!initResult) {
                controller.shutdown();
                System.exit(-3);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                private volatile boolean hasShutdown = false;
                private AtomicInteger shutdownTimes = new AtomicInteger(0);

                @Override
                public void run() {
                    synchronized (this) {
                        log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
                        if (!this.hasShutdown) {
                            this.hasShutdown = true;
                            long beginTime = System.currentTimeMillis();
                            controller.shutdown();
                            long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                            log.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
                        }
                    }
                }
            }, "ShutdownHook"));

            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    private static void start(BrokerController controller) {
        try {
            controller.start();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void properties2SystemEnv(Properties properties) {
        if (properties == null) {
            return;
        }
        String rmqAddressServerDomain = properties.getProperty("rmqAddressServerDomain", MixAll.WS_DOMAIN_NAME);
        String rmqAddressServerSubGroup = properties.getProperty("rmqAddressServerSubGroup", MixAll.WS_DOMAIN_SUBGROUP);
        System.setProperty("wisp.name.server.domain", rmqAddressServerDomain);
        System.setProperty("wisp.name.server.domain.subgroup", rmqAddressServerSubGroup);
    }
}
