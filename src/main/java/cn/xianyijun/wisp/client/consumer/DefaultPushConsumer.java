package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.consumer.listener.MessageListener;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerConcurrently;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerOrderly;
import cn.xianyijun.wisp.client.consumer.rebalance.AllocateMessageQueueAveragely;
import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class DefaultPushConsumer extends ClientConfig implements PushConsumer {

    private final transient ConsumerPushDelegate consumerPushDelegate;

    private Map<String, String> subscription = new HashMap<>();

    private String consumerGroup;

    private int consumeConcurrentlyMaxSpan = 2000;

    private long adjustThreadPoolNumsThreshold = 100000;

    @Setter
    private int pullThresholdForQueue = 1000;

    @Setter
    private int pullThresholdSizeForQueue = 100;

    private long suspendCurrentQueueTimeMillis = 1000;

    private int pullThresholdForTopic = -1;

    private int consumeThreadMin = 20;

    private int consumeThreadMax = 64;

    private long consumeTimeout = 15;

    private MessageModel messageModel = MessageModel.CLUSTERING;

    private MessageListener messageListener;

    @Setter
    private OffsetStore offsetStore;

    private AllocateMessageQueueStrategy allocateMessageQueueStrategy = new AllocateMessageQueueAveragely();

    @Setter
    private ConsumeWhereEnum consumeFromWhere = ConsumeWhereEnum.CONSUME_FROM_LAST_OFFSET;

    private String consumeTimestamp = UtilAll.timeMillisToHumanString(System.currentTimeMillis() - (1000 * 60 * 30));

    private int pullThresholdSizeForTopic = -1;

    private long pullInterval = 0;

    private int consumeMessageBatchMaxSize = 1;

    private int pullBatchSize = 32;

    private boolean postSubscriptionWhenPull = false;

    private boolean unitMode = false;

    private int maxReConsumeTimes = -1;

    public DefaultPushConsumer(final String consumerGroup) {
        this(consumerGroup, null, new AllocateMessageQueueAveragely());
    }

    public DefaultPushConsumer(final String consumerGroup, RPCHook rpcHook,
                               AllocateMessageQueueStrategy allocateMessageQueueStrategy) {
        this.consumerGroup = consumerGroup;
        this.allocateMessageQueueStrategy = allocateMessageQueueStrategy;
        consumerPushDelegate = new ConsumerPushDelegate(this, rpcHook);
    }


    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {
        createTopic(key, newTopic, queueNum, 0);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {
        this.consumerPushDelegate.createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        return this.consumerPushDelegate.searchOffset(mq, timestamp);

    }

    @Override
    public long maxOffset(MessageQueue mq) throws ClientException {
        return this.consumerPushDelegate.maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws ClientException {
        return this.consumerPushDelegate.minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        return this.consumerPushDelegate.earliestMsgStoreTime(mq);
    }

    @Override
    public ExtMessage viewMessage(String offsetMsgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.consumerPushDelegate.viewMessage(offsetMsgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws ClientException, InterruptedException {
        return null;
    }

    @Override
    public ExtMessage viewMessage(String topic, String msgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return null;
    }

    @Override
    public void sendMessageBack(ExtMessage msg, int delayLevel, String brokerName)
            throws RemotingException, BrokerException, InterruptedException, ClientException {
        this.consumerPushDelegate.sendMessageBack(msg, delayLevel, brokerName);
    }

    @Override
    public void subscribe(String topic, String subExpression) throws ClientException {
        this.consumerPushDelegate.subscribe(topic, subExpression);
    }

    @Override
    public void registerMessageListener(MessageListenerConcurrently messageListener) {
        this.messageListener = messageListener;
        this.consumerPushDelegate.setMessageListenerInner(messageListener);
    }

    @Override
    public void registerMessageListener(MessageListenerOrderly messageListener) {
        this.messageListener = messageListener;
        this.consumerPushDelegate.setMessageListenerInner(messageListener);
    }

    @Override
    public void start() throws ClientException {
        this.consumerPushDelegate.start();
    }
}
