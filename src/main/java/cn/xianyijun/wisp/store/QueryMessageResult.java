package cn.xianyijun.wisp.store;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xianyijun
 */
@Getter
@Setter
public class QueryMessageResult {

    private final List<SelectMappedBufferResult> messageMappedList =
            new ArrayList<SelectMappedBufferResult>(100);

    private final List<ByteBuffer> messageBufferList = new ArrayList<ByteBuffer>(100);
    private long indexLastUpdateTimestamp;
    private long indexLastUpdatePhyOffset;

    private int bufferTotalSize = 0;

    public void release() {
        for (SelectMappedBufferResult select : this.messageMappedList) {
            select.release();
        }
    }
}
