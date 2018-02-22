package cn.xianyijun.wisp.common.stats;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class StatsSnapshot {
    private long sum;
    private double tps;
    private double avgpt;
}
