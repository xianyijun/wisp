package cn.xianyijun.wisp.namesrv.router;

import cn.xianyijun.wisp.namesrv.NameServerController;
import cn.xianyijun.wisp.remoting.ChannelEventListener;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BrokerHousekeepingService implements ChannelEventListener{
    private final NameServerController controller;

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
