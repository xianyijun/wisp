package cn.xianyijun.wisp.broker.client;

import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.utils.RemotingUtils;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ProducerManager {
    private static final long LOCK_TIMEOUT_MILLIS = 3000;
    private static final long CHANNEL_EXPIRED_TIMEOUT = 1000 * 120;
    private final Lock groupChannelLock = new ReentrantLock();
    private final HashMap<String, HashMap<Channel, ClientChannel>> groupChannelTable =
            new HashMap<>();

    public void scanNotActiveChannel() {
        try {
            if (this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    for (final Map.Entry<String, HashMap<Channel, ClientChannel>> entry : this.groupChannelTable
                            .entrySet()) {
                        final String group = entry.getKey();
                        final HashMap<Channel, ClientChannel> chlMap = entry.getValue();

                        Iterator<Map.Entry<Channel, ClientChannel>> it = chlMap.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<Channel, ClientChannel> item = it.next();
                            final ClientChannel info = item.getValue();

                            long diff = System.currentTimeMillis() - info.getLastUpdateTimestamp();
                            if (diff > CHANNEL_EXPIRED_TIMEOUT) {
                                it.remove();
                                log.warn(
                                        "SCAN: remove expired channel[{}] from ProducerManager groupChannelTable, producer group name: {}",
                                        RemotingHelper.parseChannelRemoteAddr(info.getChannel()), group);
                                RemotingUtils.closeChannel(info.getChannel());
                            }
                        }
                    }
                } finally {
                    this.groupChannelLock.unlock();
                }
            } else {
                log.warn("ProducerManager scanNotActiveChannel lock timeout");
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public void doChannelCloseEvent(final String remoteAddr, final Channel channel) {
        if (channel == null) {
            return;
        }
        try {
            if (!this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                log.warn("ProducerManager doChannelCloseEvent lock timeout");
                return;
            }
            try {
                for (final Map.Entry<String, HashMap<Channel, ClientChannel>> entry : this.groupChannelTable
                        .entrySet()) {
                    final String group = entry.getKey();
                    final HashMap<Channel, ClientChannel> clientChannelInfoTable =
                            entry.getValue();
                    final ClientChannel clientChannel =
                            clientChannelInfoTable.remove(channel);
                    if (clientChannel != null) {
                        log.info(
                                "NETTY EVENT: remove channel[{}][{}] from ProducerManager groupChannelTable, producer group: {}",
                                clientChannel.toString(), remoteAddr, group);
                    }

                }
            } finally {
                this.groupChannelLock.unlock();
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public void registerProducer(final String group, final ClientChannel clientChannel) {
        try {
            ClientChannel clientChannelFound;

            if (this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    HashMap<Channel, ClientChannel> channelTable = this.groupChannelTable.computeIfAbsent(group, k -> new HashMap<>());

                    clientChannelFound = channelTable.get(clientChannel.getChannel());
                    if (null == clientChannelFound) {
                        channelTable.put(clientChannel.getChannel(), clientChannel);
                        log.info("new producer connected, group: {} channel: {}", group,
                                clientChannel.toString());
                    }
                } finally {
                    this.groupChannelLock.unlock();
                }

                if (clientChannelFound != null) {
                    clientChannelFound.setLastUpdateTimestamp(System.currentTimeMillis());
                }
            } else {
                log.warn("ProducerManager registerProducer lock timeout");
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public void unregisterProducer(final String group, final ClientChannel clientChannel) {
        try {
            if (this.groupChannelLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    HashMap<Channel, ClientChannel> channelTable = this.groupChannelTable.get(group);
                    if (null != channelTable && !channelTable.isEmpty()) {
                        ClientChannel old = channelTable.remove(clientChannel.getChannel());
                        if (old != null) {
                            log.info("unregister a producer[{}] from groupChannelTable {}", group,
                                    clientChannel.toString());
                        }

                        if (channelTable.isEmpty()) {
                            this.groupChannelTable.remove(group);
                            log.info("unregister a producer group[{}] from groupChannelTable", group);
                        }
                    }
                } finally {
                    this.groupChannelLock.unlock();
                }
            } else {
                log.warn("ProducerManager unregisterProducer lock timeout");
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }
}
