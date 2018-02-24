package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LockBatchRequestBody extends RemotingSerializable {
    private String consumerGroup;
    private String clientId;
    private Set<MessageQueue> mqSet = new HashSet<MessageQueue>();

}
