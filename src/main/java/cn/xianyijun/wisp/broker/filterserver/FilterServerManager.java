package cn.xianyijun.wisp.broker.filterserver;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.BrokerStartup;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.utils.RemotingUtils;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class FilterServerManager {
    private static final long FILTER_SERVER_MAX_IDLE_TIME_MILLS = 30000;
    private final BrokerController brokerController;
    private final ConcurrentMap<Channel, FilterServerInfo> filterServerTable =
            new ConcurrentHashMap<>(16);

    private ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(new WispThreadFactory("FilterServerManagerScheduledThread"));


    public void doChannelCloseEvent(final String remoteAddr, final Channel channel) {
        FilterServerInfo old = this.filterServerTable.remove(channel);
        if (old != null) {
            log.warn("The Filter Server<{}> connection<{}> closed, remove it", old.getFilterServerAddr(),
                    remoteAddr);
        }
    }

    public List<String> buildNewFilterServerList() {
        List<String> addr = new ArrayList<>();
        for (Map.Entry<Channel, FilterServerInfo> next : this.filterServerTable.entrySet()) {
            addr.add(next.getValue().getFilterServerAddr());
        }
        return addr;
    }

    public void start() {
        log.info("[FilterServerManager] start");

        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                FilterServerManager.this.createFilterServer();
            } catch (Exception e) {
                log.error("", e);
            }
        }, 1000 * 5, 1000 * 30, TimeUnit.MILLISECONDS);
    }


    public void scanNotActiveChannel() {

        Iterator<Map.Entry<Channel, FilterServerInfo>> it = this.filterServerTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Channel, FilterServerInfo> next = it.next();
            long timestamp = next.getValue().getLastUpdateTimestamp();
            Channel channel = next.getKey();
            if ((System.currentTimeMillis() - timestamp) > FILTER_SERVER_MAX_IDLE_TIME_MILLS) {
                log.info("The Filter Server<{}> expired, remove it", next.getKey());
                it.remove();
                RemotingUtils.closeChannel(channel);
            }
        }
    }

    private String buildStartCommand() {
        String config = "";
        if (BrokerStartup.configFile != null) {
            config = String.format("-c %s", BrokerStartup.configFile);
        }

        if (this.brokerController.getBrokerConfig().getNameServerAddr() != null) {
            config += String.format(" -n %s", this.brokerController.getBrokerConfig().getNameServerAddr());
        }

        return String.format("sh %s/bin/startfsrv.sh %s", this.brokerController.getBrokerConfig().getWispHome(),
                config);
    }

    private void createFilterServer() {
        int more =
                this.brokerController.getBrokerConfig().getFilterServerNums() - this.filterServerTable.size();
        String cmd = this.buildStartCommand();
        for (int i = 0; i < more; i++) {
            FilterServerUtils.callShell(cmd);
        }
    }


    public void registerFilterServer(final Channel channel, final String filterServerAddr) {
        FilterServerInfo filterServerInfo = this.filterServerTable.get(channel);
        if (filterServerInfo != null) {
            filterServerInfo.setLastUpdateTimestamp(System.currentTimeMillis());
        } else {
            filterServerInfo = new FilterServerInfo();
            filterServerInfo.setFilterServerAddr(filterServerAddr);
            filterServerInfo.setLastUpdateTimestamp(System.currentTimeMillis());
            this.filterServerTable.put(channel, filterServerInfo);
            log.info("Receive a New Filter Server<{}>", filterServerAddr);
        }
    }


    @Data
    private static class FilterServerInfo {
        private String filterServerAddr;
        private long lastUpdateTimestamp;

    }
}