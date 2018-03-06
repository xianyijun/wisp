package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.Properties;

@Data
public class ConsumerRunningInfo extends RemotingSerializable {
    public static final String PROP_NAME_SERVER_ADDR = "PROP_NAME_SERVER_ADDR";
    public static final String PROP_THREAD_POOL_CORE_SIZE = "PROP_THREAD_POOL_CORE_SIZE";
    public static final String PROP_CONSUME_ORDERLY = "PROP_CONSUME_ORDERLY";
    public static final String PROP_CONSUME_TYPE = "PROP_CONSUME_TYPE";
    public static final String PROP_CLIENT_VERSION = "PROP_CLIENT_VERSION";
    public static final String PROP_CONSUMER_START_TIMESTAMP = "PROP_CONSUMER_START_TIMESTAMP";

    private Properties properties = new Properties();

    private String jStack;

}
