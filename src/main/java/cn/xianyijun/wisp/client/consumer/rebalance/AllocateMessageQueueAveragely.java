package cn.xianyijun.wisp.client.consumer.rebalance;

import cn.xianyijun.wisp.client.consumer.AllocateMessageQueueStrategy;
import cn.xianyijun.wisp.common.message.MessageQueue;

import java.util.List;

public class AllocateMessageQueueAveragely implements AllocateMessageQueueStrategy {
    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll, List<String> cidAll) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
