package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.ExtMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Getter
@Slf4j
public class ProcessQueue {
    private final TreeMap<Long, ExtMessage> consumingMsgOrderlyTreeMap = new TreeMap<>();
    private final AtomicLong tryUnlockTimes = new AtomicLong(0);
    private volatile long queueOffsetMax = 0L;
    private volatile boolean dropped = false;
    private volatile long lastPullTimestamp = System.currentTimeMillis();
    private volatile long lastConsumeTimestamp = System.currentTimeMillis();
    private volatile boolean locked = false;
    private volatile long lastLockTimestamp = System.currentTimeMillis();
    private volatile boolean consuming = false;
    private volatile long msgAccCnt = 0;
}
