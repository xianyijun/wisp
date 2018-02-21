package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.DataVersion;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.subscription.SubscriptionGroupConfig;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
public class SubscriptionGroupWrapper extends RemotingSerializable {
    private ConcurrentMap<String, SubscriptionGroupConfig> subscriptionGroupTable =
            new ConcurrentHashMap<String, SubscriptionGroupConfig>(1024);
    private DataVersion dataVersion = new DataVersion();

}
