package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class GetConsumerStatusBody extends RemotingSerializable {
    private Map<MessageQueue, Long> messageQueueTable = new HashMap<>();
    private Map<String, Map<MessageQueue, Long>> consumerTable =
            new HashMap<>();
}
