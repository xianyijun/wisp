package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.Map;

@Data
public class ResetOffsetBody extends RemotingSerializable {
    private Map<MessageQueue, Long> offsetTable;
}
