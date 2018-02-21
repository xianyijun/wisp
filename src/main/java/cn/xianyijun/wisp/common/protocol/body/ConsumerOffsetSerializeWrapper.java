package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
public class ConsumerOffsetSerializeWrapper extends RemotingSerializable {
    private ConcurrentMap<String, ConcurrentMap<Integer, Long>> offsetTable =
            new ConcurrentHashMap<>(512);
}
