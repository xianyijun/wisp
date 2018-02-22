package cn.xianyijun.wisp.common.protocol.route;

import lombok.Data;

import java.util.HashMap;
import java.util.Random;

@Data
public class BrokerData implements Comparable<BrokerData> {
    private final Random random = new Random();
    private String cluster;
    private String brokerName;
    private HashMap<Long, String> brokerAddrs;

    @Override
    public int compareTo(BrokerData o) {
        return this.brokerName.compareTo(o.getBrokerName());
    }
}
