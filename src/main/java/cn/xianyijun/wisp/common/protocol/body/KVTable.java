package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashMap;

@Data
public class KVTable extends RemotingSerializable {

    private HashMap<String, String> table = new HashMap<>();

}
