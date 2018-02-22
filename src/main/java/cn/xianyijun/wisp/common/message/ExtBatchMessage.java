package cn.xianyijun.wisp.common.message;

import lombok.Data;

import java.nio.ByteBuffer;

/**
 * @author xianyijun
 */
@Data
public class ExtBatchMessage extends ExtMessage {

    public ByteBuffer wrap() {
        return ByteBuffer.wrap(getBody(), 0, getBody().length);
    }

    private ByteBuffer encodedBuff;
}
