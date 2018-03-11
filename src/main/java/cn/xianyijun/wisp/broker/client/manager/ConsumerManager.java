package cn.xianyijun.wisp.broker.client.manager;

import cn.xianyijun.wisp.broker.client.ClientChannel;
import cn.xianyijun.wisp.broker.client.ConsumerGroup;
import cn.xianyijun.wisp.broker.client.ConsumerGroupEvent;
import cn.xianyijun.wisp.broker.client.listener.ConsumerIdsChangeListener;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.utils.RemotingUtils;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
@Getter
public class ConsumerManager {
    private static final long CHANNEL_EXPIRED_TIMEOUT = 1000 * 120;
    private final ConsumerIdsChangeListener consumerIdsChangeListener;
    private final ConcurrentMap<String, ConsumerGroup> consumerTable =
            new ConcurrentHashMap<>(1024);

    public void scanNotActiveChannel() {
        Iterator<Map.Entry<String, ConsumerGroup>> it = this.consumerTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConsumerGroup> next = it.next();
            String group = next.getKey();
            ConsumerGroup consumerGroup = next.getValue();
            ConcurrentMap<Channel, ClientChannel> channelInfoTable =
                    consumerGroup.getChannelInfoTable();

            Iterator<Map.Entry<Channel, ClientChannel>> itChannel = channelInfoTable.entrySet().iterator();
            while (itChannel.hasNext()) {
                Map.Entry<Channel, ClientChannel> nextChannel = itChannel.next();
                ClientChannel clientChannel = nextChannel.getValue();
                long diff = System.currentTimeMillis() - clientChannel.getLastUpdateTimestamp();
                if (diff > CHANNEL_EXPIRED_TIMEOUT) {
                    log.warn(
                            "SCAN: remove expired channel from ConsumerManager consumerTable. channel={}, consumerGroup={}",
                            RemotingHelper.parseChannelRemoteAddr(clientChannel.getChannel()), group);
                    RemotingUtils.closeChannel(clientChannel.getChannel());
                    itChannel.remove();
                }
            }

            if (channelInfoTable.isEmpty()) {
                log.warn(
                        "SCAN: remove expired channel from ConsumerManager consumerTable, all clear, consumerGroup={}",
                        group);
                it.remove();
            }
        }
    }

    public ConsumerGroup getConsumerGroupInfo(final String group) {
        return this.consumerTable.get(group);
    }

    public SubscriptionData findSubscriptionData(final String group, final String topic) {
        ConsumerGroup consumerGroup = this.getConsumerGroupInfo(group);
        if (consumerGroup != null) {
            return consumerGroup.findSubscriptionData(topic);
        }

        return null;
    }

    public int findSubscriptionDataCount(final String group) {
        ConsumerGroup consumerGroup = this.getConsumerGroupInfo(group);
        if (consumerGroup != null) {
            return consumerGroup.getSubscriptionTable().size();
        }
        return 0;
    }

    public HashSet<String> queryTopicConsumeByWho(final String topic) {
        HashSet<String> groups = new HashSet<>();
        for (Map.Entry<String, ConsumerGroup> entry : this.consumerTable.entrySet()) {
            ConcurrentMap<String, SubscriptionData> subscriptionTable =
                    entry.getValue().getSubscriptionTable();
            if (subscriptionTable.containsKey(topic)) {
                groups.add(entry.getKey());
            }
        }
        return groups;
    }


    public ClientChannel findChannel(final String group, final String clientId) {
        ConsumerGroup consumerGroup = this.consumerTable.get(group);
        if (consumerGroup != null) {
            return consumerGroup.findChannel(clientId);
        }
        return null;
    }

    public void doChannelCloseEvent(final String remoteAddr, final Channel channel) {
        for (Map.Entry<String, ConsumerGroup> next : this.consumerTable.entrySet()) {
            ConsumerGroup info = next.getValue();
            boolean removed = info.doChannelCloseEvent(remoteAddr, channel);
            if (removed) {
                if (info.getChannelInfoTable().isEmpty()) {
                    ConsumerGroup remove = this.consumerTable.remove(next.getKey());
                    if (remove != null) {
                        log.info("unregister consumer ok, no any connection, and remove consumer group, {}",
                                next.getKey());
                        this.consumerIdsChangeListener.handle(ConsumerGroupEvent.UNREGISTER, next.getKey());
                    }
                }

                this.consumerIdsChangeListener.handle(ConsumerGroupEvent.CHANGE, next.getKey(), info.getAllChannel());
            }
        }
    }

    public boolean registerConsumer(final String group, final ClientChannel clientChannel,
                                    ConsumeType consumeType, MessageModel messageModel, ConsumeWhereEnum consumeFromWhere,
                                    final Set<SubscriptionData> subList, boolean isNotifyConsumerIdsChangedEnable) {

        ConsumerGroup consumerGroup = this.consumerTable.get(group);
        if (null == consumerGroup) {
            ConsumerGroup tmp = new ConsumerGroup(group, consumeType, messageModel, consumeFromWhere);
            ConsumerGroup prev = this.consumerTable.putIfAbsent(group, tmp);
            consumerGroup = prev != null ? prev : tmp;
        }

        boolean r1 =
                consumerGroup.updateChannel(clientChannel, consumeType, messageModel,
                        consumeFromWhere);
        boolean r2 = consumerGroup.updateSubscription(subList);

        if (r1 || r2) {
            if (isNotifyConsumerIdsChangedEnable) {
                this.consumerIdsChangeListener.handle(ConsumerGroupEvent.CHANGE, group, consumerGroup.getAllChannel());
            }
        }

        this.consumerIdsChangeListener.handle(ConsumerGroupEvent.REGISTER, group, subList);

        return r1 || r2;
    }

    public void unregisterConsumer(final String group, final ClientChannel clientChannel,
                                   boolean isNotifyConsumerIdsChangedEnable) {
        ConsumerGroup consumerGroup = this.consumerTable.get(group);
        if (null != consumerGroup) {
            consumerGroup.unregisterChannel(clientChannel);
            if (consumerGroup.getChannelInfoTable().isEmpty()) {
                ConsumerGroup remove = this.consumerTable.remove(group);
                if (remove != null) {
                    log.info("unregister consumer ok, no any connection, and remove consumer group, {}", group);

                    this.consumerIdsChangeListener.handle(ConsumerGroupEvent.UNREGISTER, group);
                }
            }
            if (isNotifyConsumerIdsChangedEnable) {
                this.consumerIdsChangeListener.handle(ConsumerGroupEvent.CHANGE, group, consumerGroup.getAllChannel());
            }
        }
    }

}
