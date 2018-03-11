package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * @author xianyijun
 */
@Slf4j
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
        this.consumerPullDelegate.getOffsetStore().persist(mq);
        this.consumerPullDelegate.getOffsetStore().removeOffset(mq);
        return true;
    }

    @Override
    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_ACTIVELY;
    }

    @Override
    public void removeDirtyOffset(MessageQueue mq) {
        this.consumerPullDelegate.getOffsetStore().removeOffset(mq);
    }

    @Override
    public long computePullFromWhere(MessageQueue mq) {
        return 0;
    }

    @Override
    public void messageQueueChanged(String topic, Set<MessageQueue> mqAll, Set<MessageQueue> mqDivided) {
        MessageQueueListener messageQueueListener = this.consumerPullDelegate.getDefaultPullConsumer().getMessageQueueListener();
        if (messageQueueListener != null) {
            try {
                messageQueueListener.messageQueueChanged(topic, mqAll, mqDivided);
            } catch (Throwable e) {
                log.error("messageQueueChanged exception", e);
            }
        }
    }

    @Override
    public void dispatchPullRequest(List<PullRequest> pullRequestList) {

    }
}
