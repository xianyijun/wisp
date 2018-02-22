package cn.xianyijun.wisp.store;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.MappedByteBuffer;

/**
 * @author xianyijun
 */
@Slf4j
@Data
public class StoreCheckpoint {
    private final MappedByteBuffer mappedByteBuffer;

    private volatile long indexMsgTimestamp = 0;
    private volatile long physicMsgTimestamp = 0;
    private volatile long logicMsgTimestamp = 0;


    public void flush() {
        this.mappedByteBuffer.putLong(0, this.physicMsgTimestamp);
        this.mappedByteBuffer.putLong(8, this.logicMsgTimestamp);
        this.mappedByteBuffer.putLong(16, this.indexMsgTimestamp);
        this.mappedByteBuffer.force();
    }
}
