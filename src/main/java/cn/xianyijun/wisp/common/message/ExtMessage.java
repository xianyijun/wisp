package cn.xianyijun.wisp.common.message;

import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.common.sysflag.MessageSysFlag;
import lombok.Data;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author xianyijun
 */
@Data
public class ExtMessage extends Message {

    private int queueId;

    private int sysFlag;

    private int storeSize;

    private long queueOffset;
    private long bornTimestamp;
    private SocketAddress bornHost;

    private long storeTimestamp;
    private SocketAddress storeHost;
    private String msgId;
    private long commitLogOffset;
    private int bodyCRC;
    private int reConsumeTimes;

    private long preparedTransactionOffset;


    public static TopicFilterType parseTopicFilterType(final int sysFlag) {
        if ((sysFlag & MessageSysFlag.MULTI_TAGS_FLAG) == MessageSysFlag.MULTI_TAGS_FLAG) {
            return TopicFilterType.MULTI_TAG;
        }
        return TopicFilterType.SINGLE_TAG;
    }

    public ByteBuffer getStoreHostBytes() {
        return socketAddress2ByteBuffer(this.storeHost);
    }

    public ByteBuffer getStoreHostBytes(ByteBuffer byteBuffer) {
        return socketAddress2ByteBuffer(this.storeHost, byteBuffer);
    }

    private static ByteBuffer socketAddress2ByteBuffer(final SocketAddress socketAddress, final ByteBuffer byteBuffer) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        byteBuffer.put(inetSocketAddress.getAddress().getAddress(), 0, 4);
        byteBuffer.putInt(inetSocketAddress.getPort());
        byteBuffer.flip();
        return byteBuffer;
    }

    private static ByteBuffer socketAddress2ByteBuffer(SocketAddress socketAddress) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        return socketAddress2ByteBuffer(socketAddress, byteBuffer);
    }


    public ByteBuffer getBornHostBytes() {
        return socketAddress2ByteBuffer(this.bornHost);
    }

    public ByteBuffer getBornHostBytes(ByteBuffer byteBuffer) {
        return socketAddress2ByteBuffer(this.bornHost, byteBuffer);
    }
}
