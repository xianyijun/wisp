package cn.xianyijun.wisp.common.stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Getter
public class MomentStatsItem {
    private final AtomicLong value = new AtomicLong(0);

    private final String statsName;
    private final String statsKey;
    private final ScheduledExecutorService scheduledExecutorService;
}
