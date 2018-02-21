package cn.xianyijun.wisp.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.ByteBuffer;

/**
 * @author xianyijun
 */
@AllArgsConstructor
@Getter
public class SelectMappedBufferResult {

    private final long startOffset;

    private final ByteBuffer byteBuffer;

    private int size;

    private MappedFile mappedFile;

    public synchronized void release() {
        if (this.mappedFile != null) {
            this.mappedFile.release();
            this.mappedFile = null;
        }
    }
}
