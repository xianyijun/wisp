package cn.xianyijun.wisp.common.stats;

import lombok.Data;

@Data
public class StatsSnapshot {
    private long sum;
    private double tps;
    private double avgpt;
}
