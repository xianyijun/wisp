package cn.xianyijun.wisp.broker.mqtrace;

import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.Data;

import java.util.Map;

/**
 * @author xianyijun
 */
@Data
public class ConsumeMessageContext {
    private String consumerGroup;
    private String topic;
    private Integer queueId;
    private String clientHost;
    private String storeHost;
    private Map<String, Long> messageIds;
    private int bodyLength;
    private boolean success;
    private String status;
    private Object mqTraceContext;

    private String commercialOwner;
    private BrokerStatsManager.StatsType commercialRcvStats;
    private int commercialRcvTimes;
    private int commercialRcvSize;
}
