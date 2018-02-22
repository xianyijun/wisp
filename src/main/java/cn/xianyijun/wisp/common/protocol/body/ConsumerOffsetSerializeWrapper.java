package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConsumerOffsetSerializeWrapper extends RemotingSerializable {
    private ConcurrentMap<String, ConcurrentMap<Integer, Long>> offsetTable =
            new ConcurrentHashMap<>(512);
}
