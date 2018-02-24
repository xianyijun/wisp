package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
/**
 * @author xianyijun
 */
@Data
public class LockBatchResponseBody extends RemotingSerializable{
    private Set<MessageQueue> lockOKMQSet = new HashSet<>();
}
