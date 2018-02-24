package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import lombok.Data;

import java.util.List;

@Data
public class QueryConsumeQueueResponseBody extends RemotingSerializable {

    private SubscriptionData subscriptionData;
    private String filterData;
    private List<ConsumeQueueData> queueData;
    private long maxQueueIndex;
    private long minQueueIndex;
}
