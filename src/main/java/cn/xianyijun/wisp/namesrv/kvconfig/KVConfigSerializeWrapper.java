package cn.xianyijun.wisp.namesrv.kvconfig;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;

/**
 * The type Kv config serialize wrapper.
 *
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KVConfigSerializeWrapper extends RemotingSerializable {

    private HashMap<String, HashMap<String, String>> configTable;
}
