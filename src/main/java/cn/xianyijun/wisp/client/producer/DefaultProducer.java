package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.client.ClientConfig;
import cn.xianyijun.wisp.client.QueryResult;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.Message;
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
        return null;
    }

    @Override
    public void send(Message msg, SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public void send(Message msg, SendCallback sendCallback, long timeout) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public void sendOneWay(Message msg) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public SendResult send(Message msg, MessageQueue mq) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public SendResult send(Message msg, MessageQueue mq, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback, long timeout) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public void sendOneWay(Message msg, MessageQueue mq) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public void send(Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback, long timeout) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public void sendOneWay(Message msg, MessageQueueSelector selector, Object arg) throws ClientException, RemotingException, InterruptedException {

    }

    @Override
    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecuter tranExecuter, Object arg) throws ClientException {
        return null;
    }

    @Override
    public SendResult send(Collection<Message> msgs) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public SendResult send(Collection<Message> msgs, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public SendResult send(Collection<Message> msgs, MessageQueue mq) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public SendResult send(Collection<Message> msgs, MessageQueue mq, long timeout) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return null;
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws ClientException {

    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws ClientException {

    }

    @Override
    public long searchOffset(cn.xianyijun.wisp.common.message.MessageQueue mq, long timestamp) throws ClientException {
        return 0;
    }

    @Override
    public long maxOffset(cn.xianyijun.wisp.common.message.MessageQueue mq) throws ClientException {
        return 0;
    }

    @Override
    public long minOffset(cn.xianyijun.wisp.common.message.MessageQueue mq) throws ClientException {
        return 0;
    }

    @Override
    public long earliestMsgStoreTime(cn.xianyijun.wisp.common.message.MessageQueue mq) throws ClientException {
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

    public boolean isSendMessageWithVIPChannel() {
        return this.isVipChannelEnabled();
    }
}
