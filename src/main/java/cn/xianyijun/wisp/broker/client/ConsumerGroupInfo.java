package cn.xianyijun.wisp.broker.client;

import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
@Slf4j
public class ConsumerGroupInfo {
    private final String groupName;
    private final ConcurrentMap<String, SubscriptionData> subscriptionTable =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<Channel, ClientChannelInfo> channelInfoTable =
            new ConcurrentHashMap<>(16);
    private volatile ConsumeType consumeType;
    private volatile MessageModel messageModel;
    private volatile ConsumeWhereEnum consumeFromWhere;
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();


    public ConsumerGroupInfo(String groupName, ConsumeType consumeType, MessageModel messageModel,
                             ConsumeWhereEnum consumeFromWhere) {
        this.groupName = groupName;
        this.consumeType = consumeType;
        this.messageModel = messageModel;
        this.consumeFromWhere = consumeFromWhere;
    }

    public SubscriptionData findSubscriptionData(final String topic) {
        return this.subscriptionTable.get(topic);
    }

    public List<Channel> getAllChannel() {
        return new ArrayList<>(this.channelInfoTable.keySet());
    }

    public ClientChannelInfo findChannel(final String clientId) {
        for (Map.Entry<Channel, ClientChannelInfo> next : this.channelInfoTable.entrySet()) {
            if (next.getValue().getClientId().equals(clientId)) {
                return next.getValue();
            }
        }
        return null;
    }


    public boolean doChannelCloseEvent(final String remoteAddr, final Channel channel) {
        final ClientChannelInfo info = this.channelInfoTable.remove(channel);
        return info != null;
    }

    public boolean updateChannel(final ClientChannelInfo infoNew, ConsumeType consumeType,
                                 MessageModel messageModel, ConsumeWhereEnum consumeFromWhere) {
        boolean updated = false;
        this.consumeType = consumeType;
        this.messageModel = messageModel;
        this.consumeFromWhere = consumeFromWhere;

        ClientChannelInfo infoOld = this.channelInfoTable.get(infoNew.getChannel());
        if (null == infoOld) {
            ClientChannelInfo prev = this.channelInfoTable.put(infoNew.getChannel(), infoNew);
            if (null == prev) {
                log.info("new consumer connected, group: {} {} {} channel: {}", this.groupName, consumeType,
                        messageModel, infoNew.toString());
                updated = true;
            }

            infoOld = infoNew;
        } else {
            if (!infoOld.getClientId().equals(infoNew.getClientId())) {
                log.error("[BUG] consumer channel exist in broker, but clientId not equal. GROUP: {} OLD: {} NEW: {} ",
                        this.groupName,
                        infoOld.toString(),
                        infoNew.toString());
                this.channelInfoTable.put(infoNew.getChannel(), infoNew);
            }
        }

        this.lastUpdateTimestamp = System.currentTimeMillis();
        infoOld.setLastUpdateTimestamp(this.lastUpdateTimestamp);

        return updated;
    }

    public void unregisterChannel(final ClientChannelInfo clientChannelInfo) {
        ClientChannelInfo old = this.channelInfoTable.remove(clientChannelInfo.getChannel());
        if (old != null) {
            log.info("unregister a consumer[{}] from consumerGroupInfo {}", this.groupName, old.toString());
        }
    }

    public boolean updateSubscription(final Set<SubscriptionData> subList) {
        boolean updated = false;

        for (SubscriptionData sub : subList) {
            SubscriptionData old = this.subscriptionTable.get(sub.getTopic());
            if (old == null) {
                SubscriptionData prev = this.subscriptionTable.putIfAbsent(sub.getTopic(), sub);
                if (null == prev) {
                    updated = true;
                    log.info("subscription changed, add new topic, group: {} {}",
                            this.groupName,
                            sub.toString());
                }
            } else if (sub.getSubVersion() > old.getSubVersion()) {
                if (this.consumeType == ConsumeType.CONSUME_PASSIVELY) {
                    log.info("subscription changed, group: {} OLD: {} NEW: {}",
                            this.groupName,
                            old.toString(),
                            sub.toString()
                    );
                }

                this.subscriptionTable.put(sub.getTopic(), sub);
            }
        }

        Iterator<Map.Entry<String, SubscriptionData>> it = this.subscriptionTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SubscriptionData> next = it.next();
            String oldTopic = next.getKey();

            boolean exist = false;
            for (SubscriptionData sub : subList) {
                if (sub.getTopic().equals(oldTopic)) {
                    exist = true;
                    break;
                }
            }

            if (!exist) {
                log.warn("subscription changed, group: {} remove topic {} {}",
                        this.groupName,
                        oldTopic,
                        next.getValue().toString()
                );

                it.remove();
                updated = true;
            }
        }

        this.lastUpdateTimestamp = System.currentTimeMillis();

        return updated;
    }


    public List<String> getAllClientId() {
        List<String> result = new ArrayList<>();

        for (Map.Entry<Channel, ClientChannelInfo> entry : this.channelInfoTable.entrySet()) {
            ClientChannelInfo clientChannelInfo = entry.getValue();
            result.add(clientChannelInfo.getClientId());
        }

        return result;
    }
}
