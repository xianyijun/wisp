package cn.xianyijun.wisp.broker.filterserver;

import cn.xianyijun.wisp.broker.BrokerController;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class FilterServerManager {
    private final BrokerController brokerController;

    private final ConcurrentMap<Channel, FilterServerInfo> filterServerTable =
            new ConcurrentHashMap<>(16);

    public List<String> buildNewFilterServerList() {
        List<String> addr = new ArrayList<>();
        for (Map.Entry<Channel, FilterServerInfo> next : this.filterServerTable.entrySet()) {
            addr.add(next.getValue().getFilterServerAddr());
        }
        return addr;
    }

    @AllArgsConstructor
    @Data
    private static class FilterServerInfo {
        private String filterServerAddr;
        private long lastUpdateTimestamp;

    }
}