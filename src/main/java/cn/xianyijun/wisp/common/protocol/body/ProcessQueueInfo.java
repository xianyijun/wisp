package cn.xianyijun.wisp.common.protocol.body;

import lombok.Data;


@Data
public class ProcessQueueInfo {
    private long commitOffset;

    private long cachedMsgMinOffset;
    private long cachedMsgMaxOffset;
    private int cachedMsgCount;
    private int cachedMsgSizeInMiB;

    private long transactionMsgMinOffset;
    private long transactionMsgMaxOffset;
    private int transactionMsgCount;

    private boolean locked;
    private long tryUnlockTimes;
    private long lastLockTimestamp;

    private boolean droped;
    private long lastPullTimestamp;
    private long lastConsumeTimestamp;


}
