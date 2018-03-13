package cn.xianyijun.wisp.store.result;

import cn.xianyijun.wisp.store.io.MappedFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

/**
 * @author xianyijun
 */
@AllArgsConstructor
@Getter
public class SelectMappedBufferResult {

    private final long startOffset;

    private final ByteBuffer byteBuffer;

    @Setter
    private int size;

    private MappedFile mappedFile;

    public synchronized void release() {
        if (this.mappedFile != null) {
            this.mappedFile.release();
            this.mappedFile = null;
        }
    }
}
