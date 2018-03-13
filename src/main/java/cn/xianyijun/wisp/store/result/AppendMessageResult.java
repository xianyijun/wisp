package cn.xianyijun.wisp.store.result;

import cn.xianyijun.wisp.store.status.AppendMessageStatus;
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

    public AppendMessageResult(AppendMessageStatus status) {
        this(status, 0, 0, "", 0, 0, 0);
    }

    public AppendMessageResult(AppendMessageStatus status, long wroteOffset, int wroteBytes, String msgId,
                               long storeTimestamp, long logicOffset, long pageCacheRT) {
        this.status = status;
        this.wroteOffset = wroteOffset;
        this.wroteBytes = wroteBytes;
        this.msgId = msgId;
        this.storeTimestamp = storeTimestamp;
        this.logicOffset = logicOffset;
        this.pageCacheRT = pageCacheRT;
    }

    public boolean isOk() {
        return this.status == AppendMessageStatus.PUT_OK;
    }


}
