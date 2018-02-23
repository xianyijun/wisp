package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class DefaultPushConsumer extends ClientConfig implements PushConsumer {

    private String consumerGroup;

    private long adjustThreadPoolNumsThreshold = 100000;

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
