package cn.xianyijun.wisp.broker.client;

import cn.xianyijun.wisp.remoting.protocol.LanguageCode;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class ClientChannelInfo {
    private final Channel channel;
    private final String clientId;
    private final LanguageCode language;
    private final int version;
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();

}
