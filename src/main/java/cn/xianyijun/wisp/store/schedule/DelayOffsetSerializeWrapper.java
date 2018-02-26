package cn.xianyijun.wisp.store.schedule;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type Delay offset serialize wrapper.
 *
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DelayOffsetSerializeWrapper extends RemotingSerializable {
    private ConcurrentMap<Integer /* level */, Long/* offset */> offsetTable =
            new ConcurrentHashMap<>(32);

}
