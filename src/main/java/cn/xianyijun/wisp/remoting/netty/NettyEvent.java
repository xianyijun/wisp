package cn.xianyijun.wisp.remoting.netty;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * The type Netty event.
 *
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
@ToString
public class NettyEvent {

    private final NettyEventType type;
    private final String remoteAddr;
    private final Channel channel;
}
