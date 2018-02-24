package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;

@Data
public class QueueTimeSpan {

    private MessageQueue messageQueue;
    private long minTimeStamp;
    private long maxTimeStamp;
    private long consumeTimeStamp;
    private long delayTime;
}
