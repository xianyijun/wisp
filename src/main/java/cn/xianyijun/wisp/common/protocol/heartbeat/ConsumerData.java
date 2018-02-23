package cn.xianyijun.wisp.common.protocol.heartbeat;

import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xianyijun
 */
@Data
public class ConsumerData {

    private String groupName;
    private ConsumeType consumeType;
    private MessageModel messageModel;
    private ConsumeWhereEnum consumeWhere;
    private Set<SubscriptionData> subscriptionDataSet = new HashSet<SubscriptionData>();
    private boolean unitMode;
}
