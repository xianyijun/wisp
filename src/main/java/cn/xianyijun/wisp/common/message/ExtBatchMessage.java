package cn.xianyijun.wisp.common.message;

import lombok.Data;

import java.nio.ByteBuffer;

/**
 * @author xianyijun
 */
@Data
public class ExtBatchMessage extends ExtMessage {

    private ByteBuffer encodedBuff;

    public ByteBuffer wrap() {
        return ByteBuffer.wrap(getBody(), 0, getBody().length);
    }
}
