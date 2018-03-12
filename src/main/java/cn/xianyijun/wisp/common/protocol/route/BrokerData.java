package cn.xianyijun.wisp.common.protocol.route;

import cn.xianyijun.wisp.common.MixAll;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * The type Broker data.
 *
 * @author xianyijun
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrokerData implements Comparable<BrokerData> {
    private final Random random = new Random();
    private String cluster;
    private String brokerName;
    private HashMap<Long, String> brokerAddrs;

    @Override
    public int compareTo(BrokerData o) {
        return this.brokerName.compareTo(o.getBrokerName());
    }

    /**
     * Select broker addr string.
     *
     * @return the string
     */
    public String selectBrokerAddr() {
        String addr = this.brokerAddrs.get(MixAll.MASTER_ID);

        if (addr == null) {
            List<String> addrs = new ArrayList<>(brokerAddrs.values());
            return addrs.get(random.nextInt(addrs.size()));
        }

        return addr;
    }
}
