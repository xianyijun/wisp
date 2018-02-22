package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.UtilAll;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author xianyijun
 */
@Slf4j
@Data
public class StoreCheckpoint {

    private final RandomAccessFile randomAccessFile;
    private final FileChannel fileChannel;
    private final MappedByteBuffer mappedByteBuffer;

    private volatile long indexMsgTimestamp = 0;
    private volatile long physicMsgTimestamp = 0;
    private volatile long logicMsgTimestamp = 0;

    public StoreCheckpoint(final String scpPath) throws IOException {
        File file = new File(scpPath);
        MappedFile.ensureDirOK(file.getParent());
        boolean fileExists = file.exists();

        this.randomAccessFile = new RandomAccessFile(file, "rw");
        this.fileChannel = this.randomAccessFile.getChannel();
        this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MappedFile.OS_PAGE_SIZE);

        if (fileExists) {
            log.info("store checkpoint file exists, " + scpPath);
            this.physicMsgTimestamp = this.mappedByteBuffer.getLong(0);
            this.logicMsgTimestamp = this.mappedByteBuffer.getLong(8);
            this.indexMsgTimestamp = this.mappedByteBuffer.getLong(16);

            log.info("store checkpoint file physicMsgTimestamp " + this.physicMsgTimestamp + ", "
                    + UtilAll.timeMillisToHumanString(this.physicMsgTimestamp));
            log.info("store checkpoint file logicsMsgTimestamp " + this.logicMsgTimestamp + ", "
                    + UtilAll.timeMillisToHumanString(this.logicMsgTimestamp));
            log.info("store checkpoint file indexMsgTimestamp " + this.indexMsgTimestamp + ", "
                    + UtilAll.timeMillisToHumanString(this.indexMsgTimestamp));
        } else {
            log.info("store checkpoint file not exists, " + scpPath);
        }
    }


    public void flush() {
        this.mappedByteBuffer.putLong(0, this.physicMsgTimestamp);
        this.mappedByteBuffer.putLong(8, this.logicMsgTimestamp);
        this.mappedByteBuffer.putLong(16, this.indexMsgTimestamp);
        this.mappedByteBuffer.force();
    }


    public long getMinTimestampIndex() {
        return Math.min(this.getMinTimestamp(), this.indexMsgTimestamp);
    }


    public long getMinTimestamp() {
        long min = Math.min(this.physicMsgTimestamp, this.logicMsgTimestamp);

        min -= 1000 * 3;
        if (min < 0) {
            min = 0;
        }

        return min;
    }
}
