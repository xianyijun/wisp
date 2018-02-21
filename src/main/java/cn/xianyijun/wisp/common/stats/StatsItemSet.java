package cn.xianyijun.wisp.common.stats;

import cn.xianyijun.wisp.common.UtilAll;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
public class StatsItemSet {
    private final ConcurrentMap<String, StatsItem> statsItemTable =
            new ConcurrentHashMap<>(128);

    private final String statsName;
    private final ScheduledExecutorService scheduledExecutorService;

    public StatsItemSet(String statsName, ScheduledExecutorService scheduledExecutorService) {
        this.statsName = statsName;
        this.scheduledExecutorService = scheduledExecutorService;
        this.init();
    }

    private void init() {

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                samplingInSeconds();
            } catch (Throwable ignored) {
            }
        }, 0, 10, TimeUnit.SECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                samplingInMinutes();
            } catch (Throwable ignored) {
            }
        }, 0, 10, TimeUnit.MINUTES);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                samplingInHour();
            } catch (Throwable ignored) {
            }
        }, 0, 1, TimeUnit.HOURS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                printAtMinutes();
            } catch (Throwable ignored) {
            }
        }, Math.abs(UtilAll.computeNextMinutesTimeMillis() - System.currentTimeMillis()), 1000 * 60, TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                printAtHour();
            } catch (Throwable ignored) {
            }
        }, Math.abs(UtilAll.computeNextHourTimeMillis() - System.currentTimeMillis()), 1000 * 60 * 60, TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                printAtDay();
            } catch (Throwable ignored) {
            }
        }, Math.abs(UtilAll.computeNextMorningTimeMillis() - System.currentTimeMillis()), 1000 * 60 * 60 * 24, TimeUnit.MILLISECONDS);
    }

    private void samplingInSeconds() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().samplingInSeconds();
        }
    }

    private void samplingInMinutes() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().samplingInMinutes();
        }
    }

    private void samplingInHour() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().samplingInHour();
        }
    }

    private void printAtMinutes() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtMinutes();
        }
    }

    private void printAtHour() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtHour();
        }
    }

    private void printAtDay() {
        for (Map.Entry<String, StatsItem> next : this.statsItemTable.entrySet()) {
            next.getValue().printAtDay();
        }
    }

    public void addValue(final String statsKey, final int incValue, final int incTimes) {
        StatsItem statsItem = this.getAndCreateStatsItem(statsKey);
        statsItem.getValue().addAndGet(incValue);
        statsItem.getTimes().addAndGet(incTimes);
    }

    public StatsItem getAndCreateStatsItem(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null == statsItem) {
            statsItem = new StatsItem(this.statsName, statsKey, this.scheduledExecutorService);
            StatsItem prev = this.statsItemTable.put(statsKey, statsItem);

            if (null == prev) {

                // statsItem.init();
            }
        }

        return statsItem;
    }

    public StatsSnapshot getStatsDataInMinute(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInMinute();
        }
        return new StatsSnapshot();
    }

    public StatsSnapshot getStatsDataInHour(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInHour();
        }
        return new StatsSnapshot();
    }

    public StatsSnapshot getStatsDataInDay(final String statsKey) {
        StatsItem statsItem = this.statsItemTable.get(statsKey);
        if (null != statsItem) {
            return statsItem.getStatsDataInDay();
        }
        return new StatsSnapshot();
    }

    public StatsItem getStatsItem(final String statsKey) {
        return this.statsItemTable.get(statsKey);
    }


}
