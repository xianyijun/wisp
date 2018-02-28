package cn.xianyijun.wisp.common.protocol.topic;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

@Data
public class OffsetMovedEvent extends RemotingSerializable {
    private String consumerGroup;
    private MessageQueue messageQueue;
    private long offsetRequest;
    private long offsetNew;
}
