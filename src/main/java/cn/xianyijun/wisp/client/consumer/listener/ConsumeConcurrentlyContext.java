package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Data
public class ConsumeConcurrentlyContext {

    private final MessageQueue messageQueue;
    private int delayLevelWhenNextConsume = 0;
    private int ackIndex = Integer.MAX_VALUE;
}
