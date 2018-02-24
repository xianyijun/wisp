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
    public static final String GROUP_GET_FALL_SIZE = "GROUP_GET_FALL_SIZE";
    public static final String GROUP_GET_FALL_TIME = "GROUP_GET_FALL_TIME";
    public static final String GROUP_GET_NUMS = "GROUP_GET_NUMS";
    public static final String TOPIC_PUT_NUMS = "TOPIC_PUT_NUMS";
    public static final String TOPIC_PUT_SIZE = "TOPIC_PUT_SIZE";
    public static final String BROKER_PUT_NUMS = "BROKER_PUT_NUMS";
    public static final String COMMERCIAL_OWNER = "Owner";
    private final String clusterName;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory(
            "BrokerStatsThread"));

    private final MomentStatsItemSet momentStatsItemSetFallSize = new MomentStatsItemSet(GROUP_GET_FALL_SIZE, scheduledExecutorService);

    private final HashMap<String, StatsItemSet> statsTable = new HashMap<>();

    public BrokerStatsManager(String clusterName) {
        this.clusterName = clusterName;
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