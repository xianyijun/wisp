package cn.xianyijun.wisp.client.stat;

import cn.xianyijun.wisp.common.protocol.body.ConsumeStatus;
import cn.xianyijun.wisp.common.stats.StatsItemSet;
import cn.xianyijun.wisp.common.stats.StatsSnapshot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;

@Getter
@Slf4j
public class ConsumerStatsManager {
    private static final String TOPIC_AND_GROUP_CONSUME_OK_TPS = "CONSUME_OK_TPS";
    private static final String TOPIC_AND_GROUP_CONSUME_FAILED_TPS = "CONSUME_FAILED_TPS";
    private static final String TOPIC_AND_GROUP_CONSUME_RT = "CONSUME_RT";
    private static final String TOPIC_AND_GROUP_PULL_TPS = "PULL_TPS";
    private static final String TOPIC_AND_GROUP_PULL_RT = "PULL_RT";

    private final StatsItemSet topicAndGroupConsumeOKTPS;
    private final StatsItemSet topicAndGroupConsumeRT;
    private final StatsItemSet topicAndGroupConsumeFailedTPS;
    private final StatsItemSet topicAndGroupPullTPS;
    private final StatsItemSet topicAndGroupPullRT;

    public ConsumerStatsManager(final ScheduledExecutorService scheduledExecutorService) {
        this.topicAndGroupConsumeOKTPS =
                new StatsItemSet(TOPIC_AND_GROUP_CONSUME_OK_TPS, scheduledExecutorService);

        this.topicAndGroupConsumeRT =
                new StatsItemSet(TOPIC_AND_GROUP_CONSUME_RT, scheduledExecutorService);

        this.topicAndGroupConsumeFailedTPS =
                new StatsItemSet(TOPIC_AND_GROUP_CONSUME_FAILED_TPS, scheduledExecutorService);

        this.topicAndGroupPullTPS = new StatsItemSet(TOPIC_AND_GROUP_PULL_TPS, scheduledExecutorService);

        this.topicAndGroupPullRT = new StatsItemSet(TOPIC_AND_GROUP_PULL_RT, scheduledExecutorService);
    }

    public ConsumeStatus consumeStatus(final String group, final String topic) {
        ConsumeStatus cs = new ConsumeStatus();
        {
            StatsSnapshot ss = this.getPullRT(group, topic);
            if (ss != null) {
                cs.setPullRT(ss.getAvgpt());
            }
        }

        {
            StatsSnapshot ss = this.getPullTPS(group, topic);
            if (ss != null) {
                cs.setPullTPS(ss.getTps());
            }
        }

        {
            StatsSnapshot ss = this.getConsumeRT(group, topic);
            if (ss != null) {
                cs.setConsumeRT(ss.getAvgpt());
            }
        }

        {
            StatsSnapshot ss = this.getConsumeOKTPS(group, topic);
            if (ss != null) {
                cs.setConsumeOKTPS(ss.getTps());
            }
        }

        {
            StatsSnapshot ss = this.getConsumeFailedTPS(group, topic);
            if (ss != null) {
                cs.setConsumeFailedTPS(ss.getTps());
            }
        }

        {
            StatsSnapshot ss = this.topicAndGroupConsumeFailedTPS.getStatsDataInHour(topic + "@" + group);
            if (ss != null) {
                cs.setConsumeFailedMsgs(ss.getSum());
            }
        }

        return cs;
    }

    private StatsSnapshot getPullRT(final String group, final String topic) {
        return this.topicAndGroupPullRT.getStatsDataInMinute(topic + "@" + group);
    }

    private StatsSnapshot getPullTPS(final String group, final String topic) {
        return this.topicAndGroupPullTPS.getStatsDataInMinute(topic + "@" + group);
    }

    private StatsSnapshot getConsumeRT(final String group, final String topic) {
        StatsSnapshot statsData = this.topicAndGroupConsumeRT.getStatsDataInMinute(topic + "@" + group);
        if (0 == statsData.getSum()) {
            statsData = this.topicAndGroupConsumeRT.getStatsDataInHour(topic + "@" + group);
        }

        return statsData;
    }

    private StatsSnapshot getConsumeOKTPS(final String group, final String topic) {
        return this.topicAndGroupConsumeOKTPS.getStatsDataInMinute(topic + "@" + group);
    }

    private StatsSnapshot getConsumeFailedTPS(final String group, final String topic) {
        return this.topicAndGroupConsumeFailedTPS.getStatsDataInMinute(topic + "@" + group);
    }

    public void incPullRT(final String group, final String topic, final long rt) {
        this.topicAndGroupPullRT.addValue(topic + "@" + group, (int) rt, 1);
    }

    public void incPullTPS(final String group, final String topic, final long msgs) {
        this.topicAndGroupPullTPS.addValue(topic + "@" + group, (int) msgs, 1);
    }

    public void incConsumeRT(final String group, final String topic, final long rt) {
        this.topicAndGroupConsumeRT.addValue(topic + "@" + group, (int) rt, 1);
    }

    public void incConsumeOKTPS(final String group, final String topic, final long msgs) {
        this.topicAndGroupConsumeOKTPS.addValue(topic + "@" + group, (int) msgs, 1);
    }

    public void incConsumeFailedTPS(final String group, final String topic, final long msgs) {
        this.topicAndGroupConsumeFailedTPS.addValue(topic + "@" + group, (int) msgs, 1);
    }

}
