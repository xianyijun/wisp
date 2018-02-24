package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class QueryCorrectionOffsetBody extends RemotingSerializable {
    private Map<Integer, Long> correctionOffsets = new HashMap<>();
}
