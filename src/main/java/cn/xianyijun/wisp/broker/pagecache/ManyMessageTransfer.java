package cn.xianyijun.wisp.broker.pagecache;

import cn.xianyijun.wisp.store.GetMessageResult;
import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
public class ManyMessageTransfer extends AbstractReferenceCounted implements FileRegion {

    private long transferred;
    private final ByteBuffer byteBufferHeader;
    private final GetMessageResult getMessageResult;

    @Override
    public long position() {
        int pos = byteBufferHeader.position();
        List<ByteBuffer> messageBufferList = this.getMessageResult.getMessageBufferList();
        for (ByteBuffer bb : messageBufferList) {
            pos += bb.position();
        }
        return pos;
    }

    @Override
    public long transfered() {
        return transferred;
    }

    @Override
    public long count() {
        return byteBufferHeader.limit() + this.getMessageResult.getBufferTotalSize();
    }

    @Override
    public long transferTo(WritableByteChannel target, long position) throws IOException {
        if (this.byteBufferHeader.hasRemaining()) {
            transferred += target.write(this.byteBufferHeader);
            return transferred;
        } else {
            List<ByteBuffer> messageBufferList = this.getMessageResult.getMessageBufferList();
            for (ByteBuffer bb : messageBufferList) {
                if (bb.hasRemaining()) {
                    transferred += target.write(bb);
                    return transferred;
                }
            }
        }

        return 0;
    }

    public void close() {
        this.deallocate();
    }

    @Override
    protected void deallocate() {
        this.getMessageResult.release();
    }


}