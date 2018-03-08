package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;

import java.util.List;
import java.util.Set;

/**
 * todo
 * @author xianyijun
 */
public class PullReBalance extends AbstractReBalance {

    private final ConsumerPullDelegate consumerPullDelegate;

    public PullReBalance(ConsumerPullDelegate consumerPullDelegate) {
        this(null, null, null, null, consumerPullDelegate);
    }

    public PullReBalance(String consumerGroup, MessageModel messageModel,
                             AllocateMessageQueueStrategy allocateMessageQueueStrategy,
                             ClientInstance clientFactory, ConsumerPullDelegate consumerPullDelegate) {
        super(consumerGroup, messageModel, allocateMessageQueueStrategy, clientFactory);
        this.consumerPullDelegate = consumerPullDelegate;
    }

    @Override
    public boolean removeUnnecessaryMessageQueue(MessageQueue mq, ProcessQueue pq) {
        return false;
    }

    @Override
    public ConsumeType consumeType() {
        return null;
    }

    @Override
    public void removeDirtyOffset(MessageQueue mq) {

    }

    @Override
    public long computePullFromWhere(MessageQueue mq) {
        return 0;
    }

    @Override
    public void messageQueueChanged(String topic, Set<MessageQueue> mqAll, Set<MessageQueue> mqDivided) {

    }

    @Override
    public void dispatchPullRequest(List<PullRequest> pullRequestList) {

    }
}
