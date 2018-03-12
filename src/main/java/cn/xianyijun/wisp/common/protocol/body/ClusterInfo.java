package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.route.BrokerData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClusterInfo extends RemotingSerializable {
    private HashMap<String/* brokerName */, BrokerData> brokerAddrTable;
    private HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;

    public String[] retrieveAllClusterNames() {
        return clusterAddrTable.keySet().toArray(new String[] {});
    }

    public String[] retrieveAllAddrByCluster(String cluster) {
        List<String> addrs = new ArrayList<>();
        if (clusterAddrTable.containsKey(cluster)) {
            Set<String> brokerNames = clusterAddrTable.get(cluster);
            for (String brokerName : brokerNames) {
                BrokerData brokerData = brokerAddrTable.get(brokerName);
                if (null != brokerData) {
                    addrs.addAll(brokerData.getBrokerAddrs().values());
                }
            }
        }
        return addrs.toArray(new String[] {});
    }
}
