package cn.xianyijun.wisp.common.protocol.body;

import lombok.Data;

@Data
public class BrokerStatsItem {

    private long sum;
    private double tps;
    private double avgpt;
}
