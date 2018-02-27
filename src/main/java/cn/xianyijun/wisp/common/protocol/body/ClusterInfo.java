package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.route.BrokerData;
import lombok.Data;

import java.util.HashMap;
import java.util.Set;

@Data
public class ClusterInfo extends RemotingSerializable {
    private HashMap<String/* brokerName */, BrokerData> brokerAddrTable;
    private HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;
}
