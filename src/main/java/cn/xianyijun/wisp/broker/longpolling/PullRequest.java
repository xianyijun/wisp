package cn.xianyijun.wisp.broker.longpolling;

import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.store.MessageFilter;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class PullRequest {

    private final RemotingCommand requestCommand;
    private final Channel clientChannel;
    private final long timeoutMillis;
    private final long suspendTimestamp;
    private final long pullFromThisOffset;
    private final SubscriptionData subscriptionData;
    private final MessageFilter messageFilter;
}
