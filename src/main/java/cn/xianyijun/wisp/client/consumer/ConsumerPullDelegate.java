package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.broker.mqtrace.ConsumeMessageHook;
import cn.xianyijun.wisp.client.consumer.store.OffsetStore;
import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.ServiceState;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumerRunningInfo;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Set;

/**
 * todo
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class ConsumerPullDelegate implements ConsumerInner {

    private final DefaultPullConsumer defaultMQPullConsumer;
    private final long consumerStartTimestamp = System.currentTimeMillis();
    private final RPCHook rpcHook;
    private final ArrayList<ConsumeMessageHook> consumeMessageHookList = new ArrayList<ConsumeMessageHook>();
    private final ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<FilterMessageHook>();
    private volatile ServiceState serviceState = ServiceState.CREATE_JUST;
    private ClientInstance clientFactory;
    private PullConsumerWrapper pullAPIWrapper;
    private OffsetStore offsetStore;
    private AbstractReBalance reBalance = new PullReBalance(this);



    @Override
    public String groupName() {
        return null;
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
    public Set<SubscriptionData> subscription() {
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
}
