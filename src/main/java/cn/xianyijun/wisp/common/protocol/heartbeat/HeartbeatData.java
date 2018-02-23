package cn.xianyijun.wisp.common.protocol.heartbeat;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class HeartbeatData extends RemotingSerializable {
    private String clientID;
    private Set<ProducerData> producerDataSet = new HashSet<>();
    private Set<ConsumerData> consumerDataSet = new HashSet<ConsumerData>();

}
