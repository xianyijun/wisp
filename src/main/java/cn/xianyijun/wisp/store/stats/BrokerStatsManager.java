package cn.xianyijun.wisp.store.stats;

import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.common.stats.MomentStatsItemSet;
import cn.xianyijun.wisp.common.stats.StatsItem;
import cn.xianyijun.wisp.common.stats.StatsItemSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class BrokerStatsManager {

    public static final double SIZE_PER_COUNT = 64 * 1024;
    public static final String COMMERCIAL_OWNER = "Owner";

    public static final String GROUP_GET_FALL_SIZE = "GROUP_GET_FALL_SIZE";
    public static final String GROUP_GET_FALL_TIME = "GROUP_GET_FALL_TIME";
    public static final String GROUP_GET_NUMS = "GROUP_GET_NUMS";

    public static final String SNDBCK_PUT_NUMS = "SNDBCK_PUT_NUMS";

    public static final String TOPIC_PUT_NUMS = "TOPIC_PUT_NUMS";
    public static final String TOPIC_PUT_SIZE = "TOPIC_PUT_SIZE";

    public static final String GROUP_GET_LATENCY = "GROUP_GET_LATENCY";
    public static final String GROUP_GET_SIZE = "GROUP_GET_SIZE";

    public static final String BROKER_PUT_NUMS = "BROKER_PUT_NUMS";
    public static final String BROKER_GET_NUMS = "BROKER_GET_NUMS";

    public static final String GROUP_GET_FROM_DISK_NUMS = "GROUP_GET_FROM_DISK_NUMS";
    public static final String GROUP_GET_FROM_DISK_SIZE = "GROUP_GET_FROM_DISK_SIZE";
    public static final String BROKER_GET_FROM_DISK_NUMS = "BROKER_GET_FROM_DISK_NUMS";
    public static final String BROKER_GET_FROM_DISK_SIZE = "BROKER_GET_FROM_DISK_SIZE";

    public static final String COMMERCIAL_SEND_TIMES = "COMMERCIAL_SEND_TIMES";
    public static final String COMMERCIAL_SNDBCK_TIMES = "COMMERCIAL_SNDBCK_TIMES";
    public static final String COMMERCIAL_RCV_TIMES = "COMMERCIAL_RCV_TIMES";
    public static final String COMMERCIAL_RCV_EPOLLS = "COMMERCIAL_RCV_EPOLLS";
    public static final String COMMERCIAL_SEND_SIZE = "COMMERCIAL_SEND_SIZE";
    public static final String COMMERCIAL_RCV_SIZE = "COMMERCIAL_RCV_SIZE";
    public static final String COMMERCIAL_PERM_FAILURES = "COMMERCIAL_PERM_FAILURES";

    private final String clusterName;
    private final ScheduledExecutorService commercialExecutor = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory(
            "CommercialStatsThread"));
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory(
            "BrokerStatsThread"));

    private final MomentStatsItemSet momentStatsItemSetFallTime = new MomentStatsItemSet(GROUP_GET_FALL_TIME, scheduledExecutorService);

    private final MomentStatsItemSet momentStatsItemSetFallSize = new MomentStatsItemSet(GROUP_GET_FALL_SIZE, scheduledExecutorService);

    private final HashMap<String, StatsItemSet> statsTable = new HashMap<>();

    public BrokerStatsManager(String clusterName) {
        this.clusterName = clusterName;

        this.statsTable.put(TOPIC_PUT_NUMS, new StatsItemSet(TOPIC_PUT_NUMS, this.scheduledExecutorService));
        this.statsTable.put(TOPIC_PUT_SIZE, new StatsItemSet(TOPIC_PUT_SIZE, this.scheduledExecutorService));
        this.statsTable.put(GROUP_GET_NUMS, new StatsItemSet(GROUP_GET_NUMS, this.scheduledExecutorService));
        this.statsTable.put(GROUP_GET_SIZE, new StatsItemSet(GROUP_GET_SIZE, this.scheduledExecutorService));
        this.statsTable.put(GROUP_GET_LATENCY, new StatsItemSet(GROUP_GET_LATENCY, this.scheduledExecutorService));
        this.statsTable.put(SNDBCK_PUT_NUMS, new StatsItemSet(SNDBCK_PUT_NUMS, this.scheduledExecutorService));
        this.statsTable.put(BROKER_PUT_NUMS, new StatsItemSet(BROKER_PUT_NUMS, this.scheduledExecutorService));
        this.statsTable.put(BROKER_GET_NUMS, new StatsItemSet(BROKER_GET_NUMS, this.scheduledExecutorService));
        this.statsTable.put(GROUP_GET_FROM_DISK_NUMS, new StatsItemSet(GROUP_GET_FROM_DISK_NUMS, this.scheduledExecutorService));
        this.statsTable.put(GROUP_GET_FROM_DISK_SIZE, new StatsItemSet(GROUP_GET_FROM_DISK_SIZE, this.scheduledExecutorService));
        this.statsTable.put(BROKER_GET_FROM_DISK_NUMS, new StatsItemSet(BROKER_GET_FROM_DISK_NUMS, this.scheduledExecutorService));
        this.statsTable.put(BROKER_GET_FROM_DISK_SIZE, new StatsItemSet(BROKER_GET_FROM_DISK_SIZE, this.scheduledExecutorService));

        this.statsTable.put(COMMERCIAL_SEND_TIMES, new StatsItemSet(COMMERCIAL_SEND_TIMES, this.commercialExecutor));
        this.statsTable.put(COMMERCIAL_RCV_TIMES, new StatsItemSet(COMMERCIAL_RCV_TIMES, this.commercialExecutor));
        this.statsTable.put(COMMERCIAL_SEND_SIZE, new StatsItemSet(COMMERCIAL_SEND_SIZE, this.commercialExecutor));
        this.statsTable.put(COMMERCIAL_RCV_SIZE, new StatsItemSet(COMMERCIAL_RCV_SIZE, this.commercialExecutor));
        this.statsTable.put(COMMERCIAL_RCV_EPOLLS, new StatsItemSet(COMMERCIAL_RCV_EPOLLS, this.commercialExecutor));
        this.statsTable.put(COMMERCIAL_SNDBCK_TIMES, new StatsItemSet(COMMERCIAL_SNDBCK_TIMES, this.commercialExecutor));
        this.statsTable.put(COMMERCIAL_PERM_FAILURES, new StatsItemSet(COMMERCIAL_PERM_FAILURES, this.commercialExecutor));

    }


    public void start() {
    }

    public void incTopicPutNums(final String topic, int num, int times) {
        this.statsTable.get(TOPIC_PUT_NUMS).addValue(topic, num, times);
    }

    public void incTopicPutSize(final String topic, final int size) {
        this.statsTable.get(TOPIC_PUT_SIZE).addValue(topic, size, 1);
    }

    public void incBrokerPutNums(final int incValue) {
        this.statsTable.get(BROKER_PUT_NUMS).getAndCreateStatsItem(this.clusterName).getValue().addAndGet(incValue);
    }


    public void incGroupGetNums(final String group, final String topic, final int incValue) {
        final String statsKey = buildStatsKey(topic, group);
        this.statsTable.get(GROUP_GET_NUMS).addValue(statsKey, incValue, 1);
    }


    public void incBrokerGetNums(final int incValue) {
        this.statsTable.get(BROKER_GET_NUMS).getAndCreateStatsItem(this.clusterName).getValue().addAndGet(incValue);
    }

    public void incGroupGetSize(final String group, final String topic, final int incValue) {
        final String statsKey = buildStatsKey(topic, group);
        this.statsTable.get(GROUP_GET_SIZE).addValue(statsKey, incValue, 1);
    }

    public void incGroupGetLatency(final String group, final String topic, final int queueId, final int incValue) {
        final String statsKey = String.format("%d@%s@%s", queueId, topic, group);
        this.statsTable.get(GROUP_GET_LATENCY).addValue(statsKey, incValue, 1);
    }

    public double tpsGroupGetNums(final String group, final String topic) {
        final String statsKey = buildStatsKey(topic, group);
        return this.statsTable.get(GROUP_GET_NUMS).getStatsDataInMinute(statsKey).getTps();
    }


    public String buildStatsKey(String topic, String group) {
        return topic + "@" + group;
    }

    public StatsItem getStatsItem(final String statsName, final String statsKey) {
        try {
            return this.statsTable.get(statsName).getStatsItem(statsKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void recordDiskFallBehindTime(final String group, final String topic, final int queueId,
                                         final long fallBehind) {
        final String statsKey = String.format("%d@%s@%s", queueId, topic, group);
        this.momentStatsItemSetFallTime.getAndCreateStatsItem(statsKey).getValue().set(fallBehind);
    }


    public void recordDiskFallBehindSize(final String group, final String topic, final int queueId,
                                         final long fallBehind) {
        final String statsKey = String.format("%d@%s@%s", queueId, topic, group);
        this.momentStatsItemSetFallSize.getAndCreateStatsItem(statsKey).getValue().set(fallBehind);
    }

    public enum StatsType {
        SEND_SUCCESS,
        SEND_FAILURE,
        SEND_BACK,
        SEND_TIMER,
        SEND_TRANSACTION,
        RCV_SUCCESS,
        RCV_EPOLLS,
        PERM_FAILURE
    }
}