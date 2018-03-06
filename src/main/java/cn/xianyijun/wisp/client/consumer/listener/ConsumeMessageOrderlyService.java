package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.client.consumer.ConsumeMessageService;
import cn.xianyijun.wisp.client.consumer.ConsumerPushDelegate;
import cn.xianyijun.wisp.client.consumer.DefaultPushConsumer;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.protocol.body.ConsumeMessageDirectlyResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 * todo
 */
@Slf4j
@Getter
public class ConsumeMessageOrderlyService implements ConsumeMessageService {

    private final static long MAX_TIME_CONSUME_CONTINUOUSLY =
            Long.parseLong(System.getProperty("rocketmq.client.maxTimeConsumeContinuously", "60000"));
    private final ConsumerPushDelegate consumerPushDelegate;
    private final DefaultPushConsumer defaultPushConsumer;
    private final MessageListenerOrderly messageListener;
    private final BlockingQueue<Runnable> consumeRequestQueue;
    private final ThreadPoolExecutor consumeExecutor;
    private final String consumerGroup;
    private final MessageQueueLock messageQueueLock = new MessageQueueLock();
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile boolean stopped = false;

    public ConsumeMessageOrderlyService(ConsumerPushDelegate consumerPushDelegate,
                                        MessageListenerOrderly messageListener) {
        this.consumerPushDelegate = consumerPushDelegate;
        this.messageListener = messageListener;

        this.defaultPushConsumer = this.consumerPushDelegate.getDefaultPushConsumer();
        this.consumerGroup = this.defaultPushConsumer.getConsumerGroup();
        this.consumeRequestQueue = new LinkedBlockingQueue<>();

        this.consumeExecutor = new ThreadPoolExecutor(
                this.defaultPushConsumer.getConsumeThreadMin(),
                this.defaultPushConsumer.getConsumeThreadMax(),
                1000 * 60,
                TimeUnit.MILLISECONDS,
                this.consumeRequestQueue,
                new WispThreadFactory("ConsumeMessageThread_"));

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory("ConsumeMessageScheduledThread_"));
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void incCorePoolSize() {

    }

    @Override
    public void decCorePoolSize() {

    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(ExtMessage msg, String brokerName) {
        return null;
    }
}
