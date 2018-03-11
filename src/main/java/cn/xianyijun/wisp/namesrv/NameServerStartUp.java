package cn.xianyijun.wisp.namesrv;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.namesrv.NameServerConfig;
import cn.xianyijun.wisp.remoting.netty.NettyServerConfig;
import cn.xianyijun.wisp.utils.ServerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author xianyijun
 */
@Slf4j
public class NameServerStartUp {
    private static Properties properties = null;

    public static void main(String[] args) {
        doMain(args);
    }

    private static void doMain(String[] args) {
        System.setProperty(MixAll.WISP_HOME_PROPERTY, "/Users/xianyijun/code/git/wisp/");
        try {
            Options options = ServerUtils.buildCommandlineOptions(new Options());
            CommandLine commandLine = ServerUtils.parseCmdLine("nameServer", args, buildCommandlineOptions(options), new PosixParser());

            if (commandLine == null) {
                System.exit(-1);
                return;
            }

            NameServerConfig nameServerConfig = new NameServerConfig();
            NettyServerConfig nettyServerConfig = new NettyServerConfig();
            nettyServerConfig.setListenPort(9876);

            if (commandLine.hasOption("c")) {
                String file = commandLine.getOptionValue("c");
                if (file != null) {
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties = new Properties();
                    properties.load(in);
                    MixAll.properties2Object(properties, nameServerConfig);
                    MixAll.properties2Object(properties, nettyServerConfig);

                    nameServerConfig.setConfigStorePath(file);
                    log.info("[NameServerStartUp] doMain load properties finish , file :{}", file);
                    in.close();
                }
            }

            if (commandLine.hasOption('p')) {
                MixAll.printObjectProperties(nameServerConfig);
                MixAll.printObjectProperties(nettyServerConfig);
                System.exit(0);
            }

            MixAll.properties2Object(ServerUtils.commandLine2Properties(commandLine), nameServerConfig);

            if (nameServerConfig.getWispHome() == null) {
                log.info("Please set the {} variable in your environment to match the location of the wisp installation\n", MixAll.WISP_HOME_ENV);
                System.exit(-2);
            }

            final NameServerController controller = new NameServerController(nameServerConfig, nettyServerConfig);

            controller.getConfiguration().registerConfig(properties);

            boolean initResult = controller.initialize();

            if (!initResult) {
                controller.shutdown();
                System.exit(-3);
            }

            Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(() -> {
                controller.shutdown();
                return null;
            }));

            controller.start();

        } catch (Throwable t) {
            log.error("[NameServerStartUp] doMain failure, t:{} ", t);
            System.exit(-1);
        }
    }

    private static Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("c", "configFile", true, "Name server config properties file");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("p", "printConfigItem", false, "Print all config item");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }
}
