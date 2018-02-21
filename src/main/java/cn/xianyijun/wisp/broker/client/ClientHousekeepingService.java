package cn.xianyijun.wisp.broker.client;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.remoting.ChannelEventListener;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
public class ClientHousekeepingService implements ChannelEventListener {

    private final BrokerController brokerController;

    private ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(new WispThreadFactory("ClientHousekeepingScheduledThread"));


    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {

    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {

    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {

    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {

    }
}
