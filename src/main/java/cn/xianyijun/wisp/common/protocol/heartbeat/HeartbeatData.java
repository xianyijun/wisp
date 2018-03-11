package cn.xianyijun.wisp.common.protocol.heartbeat;

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
public class HeartbeatData extends RemotingSerializable {
    private String clientID;
    private Set<ProducerData> producerDataSet = new HashSet<>();
    private Set<ConsumerData> consumerDataSet = new HashSet<ConsumerData>();

}
