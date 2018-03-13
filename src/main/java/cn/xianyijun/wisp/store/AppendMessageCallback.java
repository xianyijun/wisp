package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.message.ExtBatchMessage;
import cn.xianyijun.wisp.store.result.AppendMessageResult;

import java.nio.ByteBuffer;

/**
 * The interface Append message callback.
 */
public interface AppendMessageCallback {
    /**
     * Do append append message result.
     *
     * @param fileFromOffset the file from offset
     * @param byteBuffer     the byte buffer
     * @param maxBlank       the max blank
     * @param msg            the msg
     * @return the append message result
     */
    AppendMessageResult doAppend(final long fileFromOffset, final ByteBuffer byteBuffer,
                                 final int maxBlank, final ExtBrokerInnerMessage msg);

    /**
     * Do append append message result.
     *
     * @param fileFromOffset  the file from offset
     * @param byteBuffer      the byte buffer
     * @param maxBlank        the max blank
     * @param extBatchMessage the ext batch message
     * @return the append message result
     */
    AppendMessageResult doAppend(final long fileFromOffset, final ByteBuffer byteBuffer,
                                 final int maxBlank, final ExtBatchMessage extBatchMessage);
}
