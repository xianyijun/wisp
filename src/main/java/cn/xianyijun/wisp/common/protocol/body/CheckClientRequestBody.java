package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import lombok.Data;

@Data
public class CheckClientRequestBody extends RemotingSerializable {
    private String clientId;
    private String group;
    private SubscriptionData subscriptionData;
}
