package cn.xianyijun.wisp.common;

import cn.xianyijun.wisp.common.annotation.ImportantField;
import cn.xianyijun.wisp.common.constant.PermName;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
@Setter
public class BrokerConfig {

    private String wispHome = System.getProperty(MixAll.WISP_HOME_PROPERTY, System.getenv(MixAll.WISP_HOME_ENV));
    @ImportantField
    private String nameServerAddr = System.getProperty(MixAll.NAME_SERVER_ADDR_PROPERTY, System.getenv(MixAll.NAME_SERVER_ADDR_ENV));
    @ImportantField
    private String brokerIP1 = RemotingUtils.getLocalAddress();
    private String brokerIP2 = RemotingUtils.getLocalAddress();
    @ImportantField
    private String brokerName = localHostName();
    @ImportantField
    private String brokerClusterName = "DefaultCluster";
    @ImportantField
    private long brokerId = MixAll.MASTER_ID;
    private int brokerPermission = PermName.PERM_READ | PermName.PERM_WRITE;
    private int defaultTopicQueueNums = 8;
    @ImportantField
    private boolean autoCreateTopicEnable = true;

    private boolean clusterTopicEnable = true;

    private boolean brokerTopicEnable = true;
    @ImportantField
    private boolean autoCreateSubscriptionGroup = true;
    private String messageStorePlugIn = "";


    private int sendMessageThreadPoolNums = 1;
    private int pullMessageThreadPoolNums = 16 + Runtime.getRuntime().availableProcessors() * 2;
    private int queryMessageThreadPoolNums = 8 + Runtime.getRuntime().availableProcessors();

    private int adminBrokerThreadPoolNums = 16;
    private int clientManageThreadPoolNums = 32;
    private int consumerManageThreadPoolNums = 32;

    private int flushConsumerOffsetInterval = 1000 * 5;

    private int flushConsumerOffsetHistoryInterval = 1000 * 60;

    @ImportantField
    private boolean rejectTransactionMessage = false;
    @ImportantField
    private boolean fetchNameServerAddrByAddressServer = false;
    private int sendThreadPoolQueueCapacity = 10000;
    private int pullThreadPoolQueueCapacity = 100000;
    private int queryThreadPoolQueueCapacity = 20000;
    private int clientManagerThreadPoolQueueCapacity = 1000000;
    private int consumerManagerThreadPoolQueueCapacity = 1000000;

    private int filterServerNums = 0;

    private boolean longPollingEnable = true;

    private long shortPollingTimeMills = 1000;

    private boolean notifyConsumerIdsChangedEnable = true;

    private boolean highSpeedMode = false;

    private boolean commercialEnable = true;
    private int commercialTimerCount = 1;
    private int commercialTransCount = 1;
    private int commercialBigCount = 1;
    private int commercialBaseCount = 1;

    private boolean transferMsgByHeap = true;
    private int maxDelayTime = 40;

    private String regionId = MixAll.DEFAULT_TRACE_REGION_ID;
    private int registerBrokerTimeoutMills = 6000;

    private boolean slaveReadEnable = false;

    private boolean disableConsumeIfConsumerReadSlowly = false;
    private long consumerFallBehindThreshold = 1024L * 1024 * 1024 * 16;

    private boolean brokerFastFailureEnable = true;
    private long waitTimeMillsInSendQueue = 200;
    private long waitTimeMillsInPullQueue = 5 * 1000;

    private long startAcceptSendRequestTimeStamp = 0L;

    private boolean traceOn = true;

    private boolean enableCalcFilterBitMap = false;

    private int expectConsumerNumUseFilter = 32;

    private int maxErrorRateOfBloomFilter = 20;

    private long filterDataCleanTimeSpan = 24 * 3600 * 1000;

    private boolean filterSupportRetry = false;

    private boolean enablePropertyFilter = false;

    private static String localHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Failed to obtain the host name", e);
        }

        return "DEFAULT_BROKER";
    }
}
