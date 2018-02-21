package cn.xianyijun.wisp.broker.mqtrace;

import cn.xianyijun.wisp.common.message.MessageType;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.Data;

import java.util.Properties;

@Data
public class ProduceMessageContext {

    private String producerGroup;
    private String topic;
    private String msgId;
    private String originMsgId;
    private Integer queueId;
    private Long queueOffset;
    private String brokerAddr;
    private String bornHost;
    private int bodyLength;
    private int code;
    private String errorMsg;
    private String msgProps;
    private Object mqTraceContext;
    private Properties extProps;
    private String brokerRegionId;
    private String msgUniqueKey;
    private long bornTimeStamp;
    private MessageType msgType = MessageType.TRANS_MSG_COMMIT;
    private boolean isSuccess = false;
    private String commercialOwner;
    private BrokerStatsManager.StatsType commercialSendStats;
    private int commercialSendSize;
    private int commercialSendTimes;

}
