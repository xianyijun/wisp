package cn.xianyijun.wisp.namesrv.router;

import cn.xianyijun.wisp.namesrv.NameServerController;
import cn.xianyijun.wisp.remoting.ChannelEventListener;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class BrokerHousekeepingService implements ChannelEventListener {
    private final NameServerController controller;

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        log.info("[BrokerHousekeepingService] onChannelConnect , remoteAddr :{} , channel :{} ",remoteAddr, channel);
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        log.info("[BrokerHousekeepingService] onChannelClose , remoteAddr :{} , channel :{} ",remoteAddr, channel);
        this.controller.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        log.info("[BrokerHousekeepingService] onChannelException , remoteAddr :{} , channel :{} ",remoteAddr, channel);
        this.controller.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        log.info("[BrokerHousekeepingService] onChannelIdle , remoteAddr :{} , channel :{} ",remoteAddr, channel);
        this.controller.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
    }
}
