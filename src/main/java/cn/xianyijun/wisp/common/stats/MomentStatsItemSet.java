package cn.xianyijun.wisp.common.stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class MomentStatsItemSet {
    private final ConcurrentMap<String, MomentStatsItem> statsItemTable =
            new ConcurrentHashMap<>(128);
    private final String statsName;
    private final ScheduledExecutorService scheduledExecutorService;


    public MomentStatsItem getAndCreateStatsItem(final String statsKey) {
        MomentStatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null == statsItem) {
            statsItem =
                    new MomentStatsItem(this.statsName, statsKey, this.scheduledExecutorService);
            this.statsItemTable.put(statsKey, statsItem);
        }
        return statsItem;
    }
}
