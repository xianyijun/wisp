package cn.xianyijun.wisp.store;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class AppendMessageResult {
    private AppendMessageStatus status;
    private long wroteOffset;
    private int wroteBytes;
    private String msgId;
    private long storeTimestamp;
    private long logicOffset;
    private long pageCacheRT = 0;

    private int msgNum = 1;
}
