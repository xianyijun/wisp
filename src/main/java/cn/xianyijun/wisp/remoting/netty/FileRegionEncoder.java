package cn.xianyijun.wisp.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * The type File region encoder.
 * @author xianyijun
 */
public class FileRegionEncoder extends MessageToByteEncoder<FileRegion> {
    @Override
    protected void encode(ChannelHandlerContext ctx, FileRegion msg, final ByteBuf out) throws Exception {
        WritableByteChannel writableByteChannel = new WritableByteChannel() {
            @Override
            public int write(ByteBuffer src) {
                out.writeBytes(src);
                return out.capacity();
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() {
            }
        };

        long toTransfer = msg.count();

        while (true) {
            long transferred = msg.transfered();
            if (toTransfer - transferred <= 0) {
                break;
            }
            msg.transferTo(writableByteChannel, transferred);
        }
    }
}
