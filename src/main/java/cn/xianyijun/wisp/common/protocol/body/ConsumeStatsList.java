package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.admin.ConsumeStats;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ConsumeStatsList extends RemotingSerializable {
    private List<Map<String/*subscriptionGroupName*/, List<ConsumeStats>>> consumeStatsList = new ArrayList<>();
    private String brokerAddr;
    private long totalDiff;
}
