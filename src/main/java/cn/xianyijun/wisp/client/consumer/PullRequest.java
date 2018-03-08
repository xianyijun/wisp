package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;

@Data
public class PullRequest {
    private String consumerGroup;
    private MessageQueue messageQueue;
    private ProcessQueue processQueue;
    private long nextOffset;
    private boolean lockedFirst = false;

}
