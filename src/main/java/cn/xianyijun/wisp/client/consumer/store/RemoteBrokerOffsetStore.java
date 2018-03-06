package cn.xianyijun.wisp.client.consumer.store;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * todo
 * @author xianyijun
 */
public class RemoteBrokerOffsetStore implements OffsetStore {
    private final ClientInstance clientFactory;
    private final String groupName;
    private ConcurrentMap<MessageQueue, AtomicLong> offsetTable =
            new ConcurrentHashMap<MessageQueue, AtomicLong>();

    public RemoteBrokerOffsetStore(ClientInstance clientFactory, String groupName) {
        this.clientFactory = clientFactory;
        this.groupName = groupName;
    }

    @Override
    public void load() throws ClientException {

    }

    @Override
    public void updateOffset(MessageQueue mq, long offset, boolean increaseOnly) {

    }

    @Override
    public long readOffset(MessageQueue mq, ReadOffsetType type) {
        return 0;
    }

    @Override
    public void persistAll(Set<MessageQueue> mqs) {

    }

    @Override
    public void persist(MessageQueue mq) {

    }

    @Override
    public void removeOffset(MessageQueue mq) {

    }

    @Override
    public Map<MessageQueue, Long> cloneOffsetTable(String topic) {
        return null;
    }

    @Override
    public void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException, BrokerException, InterruptedException, ClientException {

    }
}
