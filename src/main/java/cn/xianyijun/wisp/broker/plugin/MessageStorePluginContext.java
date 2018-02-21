package cn.xianyijun.wisp.broker.plugin;

import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.store.MessageArrivingListener;
import cn.xianyijun.wisp.store.config.MessageStoreConfig;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageStorePluginContext {
    private MessageStoreConfig messageStoreConfig;
    private BrokerStatsManager brokerStatsManager;
    private MessageArrivingListener messageArrivingListener;
    private BrokerConfig brokerConfig;
}
