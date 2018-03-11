package cn.xianyijun.wisp.broker.client;

import cn.xianyijun.wisp.remoting.protocol.LanguageCode;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class ClientChannel {
    private final Channel channel;
    private final String clientId;
    private final LanguageCode language;
    private final int version;
    @Setter
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();
}
