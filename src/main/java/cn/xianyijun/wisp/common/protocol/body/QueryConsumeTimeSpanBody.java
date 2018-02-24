package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryConsumeTimeSpanBody extends RemotingSerializable {
    List<QueueTimeSpan> consumeTimeSpanSet = new ArrayList<QueueTimeSpan>();
}
