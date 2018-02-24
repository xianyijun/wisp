package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class TopicList extends RemotingSerializable {
    private Set<String> topicList = new HashSet<String>();
    private String brokerAddr;
}
