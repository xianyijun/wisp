package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Data
public class ConsumeOrderlyContext {
    private final MessageQueue messageQueue;
    private boolean autoCommit = true;
    private long suspendCurrentQueueTimeMillis = -1;
}
