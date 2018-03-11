package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetConsumerStatusBody extends RemotingSerializable {
    private Map<MessageQueue, Long> messageQueueTable = new HashMap<>();
    private Map<String, Map<MessageQueue, Long>> consumerTable =
            new HashMap<>();
}
