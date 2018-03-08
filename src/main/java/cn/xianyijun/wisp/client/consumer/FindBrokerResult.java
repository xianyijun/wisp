package cn.xianyijun.wisp.client.consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class FindBrokerResult {
    private final String brokerAddr;
    private final boolean slave;
    private final int brokerVersion;

    public FindBrokerResult(String brokerAddr, boolean slave) {
        this.brokerAddr = brokerAddr;
        this.slave = slave;
        this.brokerVersion = 0;
    }

}
