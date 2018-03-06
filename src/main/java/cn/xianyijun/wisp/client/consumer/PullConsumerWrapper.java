package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Getter
public class PullConsumerWrapper {
    private final ClientInstance clientFactory;
    private final String consumerGroup;
    private final boolean unitMode;
    private ConcurrentMap<MessageQueue, AtomicLong/* brokerId */> pullFromWhichNodeTable =
            new ConcurrentHashMap<>(32);
    private volatile boolean connectBrokerByUser = false;
    private volatile long defaultBrokerId = MixAll.MASTER_ID;
    private Random random = new Random(System.currentTimeMillis());
    @Setter
    private ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<>();

    public PullConsumerWrapper(ClientInstance clientFactory, String consumerGroup, boolean unitMode) {
        this.clientFactory = clientFactory;
        this.consumerGroup = consumerGroup;
        this.unitMode = unitMode;
    }

}
