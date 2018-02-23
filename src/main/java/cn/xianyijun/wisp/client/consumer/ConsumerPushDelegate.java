package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
public class ConsumerPushDelegate implements ConsumerInner {

    private final DefaultPushConsumer defaultMQPushConsumer;

    private final RPCHook rpcHook;

    private ConsumeMessageService consumeMessageService;

    private AbstractReBalance reBalance = new PushReBalance(this);

    public void adjustThreadPool() {
        long computeAccTotal = this.computeAccumulationTotal();
        long adjustThreadPoolNumsThreshold = this.defaultMQPushConsumer.getAdjustThreadPoolNumsThreshold();

        long incThreshold = (long) (adjustThreadPoolNumsThreshold * 1.0);

        long decThreshold = (long) (adjustThreadPoolNumsThreshold * 0.8);

        if (computeAccTotal >= incThreshold) {
            this.consumeMessageService.incCorePoolSize();
        }

        if (computeAccTotal < decThreshold) {
            this.consumeMessageService.decCorePoolSize();
        }
    }

    @Override
    public String groupName() {
        return this.defaultMQPushConsumer.getConsumerGroup();
    }

    @Override
    public MessageModel messageModel() {
        return null;
    }

    @Override
    public ConsumeType consumeType() {
        return null;
    }

    @Override
    public ConsumeWhereEnum consumeFromWhere() {
        return null;
    }

    @Override
    public Set<SubscriptionData> subScriptions() {
        return null;
    }

    @Override
    public void doReBalance() {

    }

    @Override
    public void persistConsumerOffset() {

    }

    @Override
    public void updateTopicSubscribeInfo(String topic, Set<MessageQueue> info) {

    }

    @Override
    public boolean isSubscribeTopicNeedUpdate(String topic) {
        return false;
    }

    @Override
    public boolean isUnitMode() {
        return false;
    }

    @Override
    public ConsumerRunningInfo consumerRunningInfo() {
        return null;
    }


    private long computeAccumulationTotal() {
        long msgAccTotal = 0;
        ConcurrentMap<MessageQueue, ProcessQueue> processQueueTable = this.reBalance.getProcessQueueTable();
        for (Map.Entry<MessageQueue, ProcessQueue> next : processQueueTable.entrySet()) {
            ProcessQueue value = next.getValue();
            msgAccTotal += value.getMsgAccCnt();
        }

        return msgAccTotal;
    }
}
