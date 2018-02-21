package cn.xianyijun.wisp.common.subscription;

import cn.xianyijun.wisp.common.MixAll;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionGroupConfig {
    private String groupName;

    private boolean consumeEnable = true;
    private boolean consumeFromMinEnable = true;

    private boolean consumeBroadcastEnable = true;

    private int retryQueueNums = 1;

    private int retryMaxTimes = 16;

    private long brokerId = MixAll.MASTER_ID;

    private long whichBrokerWhenConsumeSlowly = 1;

    private boolean notifyConsumerIdsChangedEnable = true;
}
