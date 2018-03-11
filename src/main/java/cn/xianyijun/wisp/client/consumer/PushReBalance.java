package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.client.consumer.store.ReadOffsetType;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.exception.ClientException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
@Slf4j
public class PushReBalance extends AbstractReBalance {

    private final static long UNLOCK_DELAY_TIME_MILLS = Long.parseLong(System.getProperty("wisp.client.unlockDelayTimeMills", "20000"));

    private final ConsumerPushDelegate pushDelegate;

    public PushReBalance(ConsumerPushDelegate pushDelegate) {
        this(null, null, null, null, pushDelegate);
    }

    public PushReBalance(String consumerGroup, MessageModel messageModel,
                         AllocateMessageQueueStrategy allocateMessageQueueStrategy,
                         ClientInstance mQClientFactory, ConsumerPushDelegate pushDelegate) {
        super(consumerGroup, messageModel, allocateMessageQueueStrategy, mQClientFactory);
        this.pushDelegate = pushDelegate;
    }

    @Override
    public boolean removeUnnecessaryMessageQueue(MessageQueue mq, ProcessQueue pq) {
        this.pushDelegate.getOffsetStore().persist(mq);
        this.pushDelegate.getOffsetStore().removeOffset(mq);
        if (this.pushDelegate.isConsumeOrderly()
                && MessageModel.CLUSTERING.equals(this.pushDelegate.messageModel())) {
            try {
                if (pq.getLockConsume().tryLock(1000, TimeUnit.MILLISECONDS)) {
                    try {
                        return this.unlockDelay(mq, pq);
                    } finally {
                        pq.getLockConsume().unlock();
                    }
                } else {
                    log.warn("[WRONG]mq is consuming, so can not unlock it, {}. maybe hanged for a while, {}",
                            mq,
                            pq.getTryUnlockTimes());

                    pq.incTryUnlockTimes();
                }
            } catch (Exception e) {
                log.error("removeUnnecessaryMessageQueue Exception", e);
            }

            return false;
        }
        return true;
    }

    private boolean unlockDelay(final MessageQueue mq, final ProcessQueue pq) {

        if (pq.hasTempMessage()) {
            log.info("[{}]unlockDelay, begin {} ", mq.hashCode(), mq);
            this.pushDelegate.getClientFactory().getScheduledExecutorService().schedule((Runnable) () -> {
                log.info("[{}]unlockDelay, execute at once {}", mq.hashCode(), mq);
                PushReBalance.this.unLock(mq, true);
            }, UNLOCK_DELAY_TIME_MILLS, TimeUnit.MILLISECONDS);
        } else {
            this.unLock(mq, true);
        }
        return true;
    }

    @Override
    public ConsumeType consumeType() {
        return ConsumeType.CONSUME_PASSIVELY;
    }

    @Override
    public void removeDirtyOffset(MessageQueue mq) {
        this.pushDelegate.getOffsetStore().removeOffset(mq);
    }

    @Override
    public long computePullFromWhere(MessageQueue mq) {
        long result = -1;
        final ConsumeWhereEnum consumeFromWhere = this.pushDelegate.getDefaultPushConsumer().getConsumeFromWhere();
        final OffsetStore offsetStore = this.pushDelegate.getOffsetStore();
        switch (consumeFromWhere) {
            case CONSUME_FROM_LAST_OFFSET: {
                long lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0) {
                    result = lastOffset;
                } else if (-1 == lastOffset) {
                    if (mq.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                        result = 0L;
                    } else {
                        try {
                            result = this.getClientFactory().getAdmin().maxOffset(mq);
                        } catch (ClientException e) {
                            result = -1;
                        }
                    }
                } else {
                    result = -1;
                }
                break;
            }
            case CONSUME_FROM_FIRST_OFFSET: {
                long lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0) {
                    result = lastOffset;
                } else if (-1 == lastOffset) {
                    result = 0L;
                } else {
                    result = -1;
                }
                break;
            }
            case CONSUME_FROM_TIMESTAMP: {
                long lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE);
                if (lastOffset >= 0) {
                    result = lastOffset;
                } else if (-1 == lastOffset) {
                    if (mq.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                        try {
                            result = this.getClientFactory().getAdmin().maxOffset(mq);
                        } catch (ClientException e) {
                            result = -1;
                        }
                    } else {
                        try {
                            long timestamp = UtilAll.parseDate(this.pushDelegate.getDefaultPushConsumer().getConsumeTimestamp(),
                                    UtilAll.YYYYMMDDHHMMSS).getTime();
                            result = this.getClientFactory().getAdmin().searchOffset(mq, timestamp);
                        } catch (ClientException e) {
                            result = -1;
                        }
                    }
                } else {
                    result = -1;
                }
                break;
            }

            default:
                break;
        }

        return result;
    }

    @Override
    public void messageQueueChanged(String topic, Set<MessageQueue> mqAll, Set<MessageQueue> mqDivided) {
        SubscriptionData subscriptionData = this.subscriptionInner.get(topic);
        long newVersion = System.currentTimeMillis();
        log.info("{} Rebalance changed, also update version: {}, {}", topic, subscriptionData.getSubVersion(), newVersion);
        subscriptionData.setSubVersion(newVersion);

        int currentQueueCount = this.processQueueTable.size();
        if (currentQueueCount != 0) {
            int pullThresholdForTopic = this.pushDelegate.getDefaultPushConsumer().getPullThresholdForTopic();
            if (pullThresholdForTopic != -1) {
                int newVal = Math.max(1, pullThresholdForTopic / currentQueueCount);
                log.info("The pullThresholdForQueue is changed from {} to {}",
                        this.pushDelegate.getDefaultPushConsumer().getPullThresholdForQueue(), newVal);
                this.pushDelegate.getDefaultPushConsumer().setPullThresholdForQueue(newVal);
            }

            int pullThresholdSizeForTopic = this.pushDelegate.getDefaultPushConsumer().getPullThresholdSizeForTopic();
            if (pullThresholdSizeForTopic != -1) {
                int newVal = Math.max(1, pullThresholdSizeForTopic / currentQueueCount);
                log.info("The pullThresholdSizeForQueue is changed from {} to {}",
                        this.pushDelegate.getDefaultPushConsumer().getPullThresholdSizeForQueue(), newVal);
                this.pushDelegate.getDefaultPushConsumer().setPullThresholdSizeForQueue(newVal);
            }
        }

        // notify broker
        this.getClientFactory().sendHeartbeatToAllBrokerWithLock();
    }

    @Override
    public void dispatchPullRequest(List<PullRequest> pullRequestList) {
        for (PullRequest pullRequest : pullRequestList) {
            this.pushDelegate.executePullRequestImmediately(pullRequest);
            log.info("doReBalance, {}, add a new pull request {}", consumerGroup, pullRequest);
        }
    }
}
