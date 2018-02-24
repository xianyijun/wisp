package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashSet;

@Data
public class ProducerConnection extends RemotingSerializable {
    private HashSet<Connection> connectionSet = new HashSet<>();
}
