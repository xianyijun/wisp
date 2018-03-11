package cn.xianyijun.wisp.common.running;

/**
 * The enum Running stats.
 */
public enum RunningStats {
    /**
     * Commit log max offset running stats.
     */
    commitLogMaxOffset,
    /**
     * Commit log min offset running stats.
     */
    commitLogMinOffset,
    /**
     * Commit log disk ratio running stats.
     */
    commitLogDiskRatio,
    /**
     * Consume queue disk ratio running stats.
     */
    consumeQueueDiskRatio,
    /**
     * Schedule message offset running stats.
     */
    scheduleMessageOffset,
}
