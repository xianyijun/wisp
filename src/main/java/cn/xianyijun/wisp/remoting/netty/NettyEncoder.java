package cn.xianyijun.wisp.remoting.netty;

import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.utils.RemotingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * @author xianyijun
 */
@Slf4j
public class NettyEncoder  extends MessageToByteEncoder<RemotingCommand> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemotingCommand remotingCommand, ByteBuf byteBuf) throws Exception {
        try {
            ByteBuffer header = remotingCommand.encodeHeader();
            byteBuf.writeBytes(header);
            byte[] body = remotingCommand.getBody();
            if (body != null) {
                byteBuf.writeBytes(body);
            }
        } catch (Exception e) {
            log.error("encode exception, " + RemotingHelper.parseChannelRemoteAddr(channelHandlerContext.channel()), e);
            if (remotingCommand != null) {
                log.error(remotingCommand.toString());
            }
            RemotingUtils.closeChannel(channelHandlerContext.channel());
        }
    }
}
