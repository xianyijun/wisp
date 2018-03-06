package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.client.consumer.listener.MessageListener;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class DefaultPushConsumer extends ClientConfig implements PushConsumer {

    private Map<String /* topic */, String /* sub expression */> subscription = new HashMap<>();


    private String consumerGroup;

    private int consumeConcurrentlyMaxSpan = 2000;

    private long adjustThreadPoolNumsThreshold = 100000;

    private int pullThresholdForQueue = 1000;

    private int pullThresholdSizeForQueue = 100;

    private int pullThresholdForTopic = -1;

    private int consumeThreadMin = 20;

    private int consumeThreadMax = 64;

    private MessageModel messageModel = MessageModel.CLUSTERING;

    private MessageListener messageListener;

    @Setter
    private OffsetStore offsetStore;

    private AllocateMessageQueueStrategy allocateMessageQueueStrategy = new AllocateMessageQueueAveragely();

    private ConsumeWhereEnum consumeFromWhere = ConsumeWhereEnum.CONSUME_FROM_LAST_OFFSET;

    private String consumeTimestamp = UtilAll.timeMillisToHumanString(System.currentTimeMillis() - (1000 * 60 * 30));

    private int pullThresholdSizeForTopic = -1;

    private long pullInterval = 0;

    private int consumeMessageBatchMaxSize = 1;

    private int pullBatchSize = 32;

    private boolean postSubscriptionWhenPull = false;

    private boolean unitMode = false;

    private int maxReConsumeTimes = -1;

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {

    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {

    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        return 0;
    }

    @Override
    public long maxOffset(MessageQueue mq) throws ClientException {
        return 0;
    }

    @Override
    public long minOffset(MessageQueue mq) throws ClientException {
        return 0;
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        return 0;
    }

    @Override
    public ExtMessage viewMessage(String offsetMsgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return null;
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws ClientException, InterruptedException {
        return null;
    }

    @Override
    public ExtMessage viewMessage(String topic, String msgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return null;
    }
}
