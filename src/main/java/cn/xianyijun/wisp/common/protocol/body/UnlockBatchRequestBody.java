package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class UnlockBatchRequestBody extends RemotingSerializable {
    private String consumerGroup;
    private String clientId;
    private Set<MessageQueue> mqSet = new HashSet<>();
}
