package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import lombok.Data;

import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

@Data
public class ConsumerRunningInfo extends RemotingSerializable {
    public static final String PROP_NAME_SERVER_ADDR = "PROP_NAME_SERVER_ADDR";
    public static final String PROP_THREAD_POOL_CORE_SIZE = "PROP_THREAD_POOL_CORE_SIZE";
    public static final String PROP_CONSUME_ORDERLY = "PROP_CONSUME_ORDERLY";
    public static final String PROP_CONSUME_TYPE = "PROP_CONSUME_TYPE";
    public static final String PROP_CLIENT_VERSION = "PROP_CLIENT_VERSION";
    public static final String PROP_CONSUMER_START_TIMESTAMP = "PROP_CONSUMER_START_TIMESTAMP";

    private TreeSet<SubscriptionData> subscriptionSet = new TreeSet<>();

    private TreeMap<MessageQueue, ProcessQueueInfo> mqTable = new TreeMap<>();

    private Properties properties = new Properties();

    private String jStack;

    private TreeMap<String/* Topic */, ConsumeStatus> statusTable = new TreeMap<>();

}
