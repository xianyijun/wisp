package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.BatchMessage;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageClientIDSetter;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * @author xianyijun
 */
@Getter
@Slf4j
public class DefaultProducer extends ClientConfig implements Producer {

    protected final transient ProducerDelegate producerDelegate;

    private String producerGroup;

    private String createTopicKey = MixAll.DEFAULT_TOPIC;

    private volatile int defaultTopicQueueNums = 4;

    private int retryTimesWhenSendFailed = 2;

    private int sendMsgTimeout = 3000;

    private boolean retryAnotherBrokerWhenNotStoreOK = false;

    private int retryTimesWhenSendAsyncFailed = 2;

    private int compressMsgBodyOverHowMuch = 1024 * 4;

    /**
     * Constructor specifying both producer group and RPC hook.
     *
     * @param producerGroup Producer group, see the name-sake field.
     * @param rpcHook RPC hook to execute per each remoting command execution.
     */
    public DefaultProducer(final String producerGroup, RPCHook rpcHook) {
        this.producerGroup = producerGroup;
        producerDelegate = new ProducerDelegate(this, rpcHook);
    }

    /**
     * Constructor specifying producer group.
     *
     * @param producerGroup Producer group, see the name-sake field.
     */
    public DefaultProducer(final String producerGroup) {
        this(producerGroup, null);
    }

    /**
     * Constructor specifying the RPC hook.
     *
     * @param rpcHook RPC hook to execute per each remoting command execution.
     */
    public DefaultProducer(RPCHook rpcHook) {
        this(MixAll.DEFAULT_PRODUCER_GROUP, rpcHook);
    }

    @Override
    public void start() throws ClientException {
        this.producerDelegate.start();
    }

    @Override
    public void shutdown() {
        this.producerDelegate.shutdown();
    }

    @Override
    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws ClientException {
        return this.producerDelegate.fetchPublishMessageQueues(topic);
    }

    @Override
    public SendResult send(Message msg) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(msg);
    }

    @Override
    public SendResult send(Message msg, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(msg, timeout);
    }

    @Override
    public void send(Message msg, SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.send(msg, sendCallback);
    }

    @Override
    public void send(Message msg, SendCallback sendCallback, long timeout) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.send(msg, sendCallback, timeout);
    }

    @Override
    public void sendOneWay(Message msg) throws ClientException, InterruptedException {
        this.producerDelegate.sendOneWay(msg);
    }

    @Override
    public SendResult send(Message msg, MessageQueue mq) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(msg, mq);
    }

    @Override
    public SendResult send(Message msg, MessageQueue mq, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(msg, mq, timeout);
    }

    @Override
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.send(msg, mq, sendCallback);
    }

    @Override
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback, long timeout) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.send(msg, mq, sendCallback, timeout);
    }

    @Override
    public void sendOneWay(Message msg, MessageQueue mq) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.sendOneWay(msg, mq);
    }

    @Override
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(msg, selector, arg);
    }

    @Override
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(msg, selector, arg, timeout);
    }

    @Override
    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.send(msg, selector, arg, sendCallback);
    }

    @Override
    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback, long timeout) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.send(msg, selector, arg, sendCallback, timeout);
    }

    @Override
    public void sendOneWay(Message msg, MessageQueueSelector selector, Object arg) throws ClientException, RemotingException, InterruptedException {
        this.producerDelegate.sendOneWay(msg, selector, arg);
    }

    @Override
    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecutor transactionExecutor, Object arg) throws ClientException {
        throw new RuntimeException("sendMessageInTransaction not implement, please use TransactionMQProducer class");
    }

    @Override
    public SendResult send(Collection<Message> msgs) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(batch(msgs));
    }

    @Override
    public SendResult send(Collection<Message> msgs, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(batch(msgs), timeout);
    }

    @Override
    public SendResult send(Collection<Message> msgs, MessageQueue messageQueue) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(batch(msgs), messageQueue);
    }

    @Override
    public SendResult send(Collection<Message> msgs, MessageQueue messageQueue, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return this.producerDelegate.send(batch(msgs), messageQueue, timeout);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws ClientException{
        createTopic(key, newTopic, queueNum, 0);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException{
        this.producerDelegate.createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws ClientException {
        return this.producerDelegate.searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws ClientException {
        return this.producerDelegate.maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws ClientException {
        return this.producerDelegate.minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws ClientException {
        return this.producerDelegate.earliestMsgStoreTime(mq);
    }

    @Override
    public ExtMessage viewMessage(String offsetMsgId) throws RemotingException, BrokerException, InterruptedException, ClientException {
        return this.producerDelegate.viewMessage(offsetMsgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws ClientException, InterruptedException {
        return this.producerDelegate.queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    public ExtMessage viewMessage(String topic, String msgId) throws InterruptedException, ClientException {
        try {
            MessageDecoder.decodeMessageId(msgId);
            return this.viewMessage(msgId);
        } catch (Exception ignored) {
        }
        return this.producerDelegate.queryMessageByUniqueKey(topic, msgId);
    }

    public boolean isSendMessageWithVIPChannel() {
        return this.isVipChannelEnabled();
    }

    private BatchMessage batch(Collection<Message> msgs) throws ClientException {
        BatchMessage msgBatch;
        try {
            msgBatch = BatchMessage.generateFromList(msgs);
            for (Message message : msgBatch) {
                MessageClientIDSetter.setUniqueID(message);
            }
            msgBatch.setBody(msgBatch.encode());
        } catch (Exception e) {
            throw new ClientException("Failed to initiate the MessageBatch", e);
        }
        return msgBatch;
    }

}
