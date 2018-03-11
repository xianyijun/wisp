package cn.xianyijun.wisp.store;

import lombok.Getter;

@Getter
public class RunningFlags {

    private static final int NOT_READABLE_BIT = 1;

    private static final int NOT_WRITEABLE_BIT = 1 << 1;

    private static final int WRITE_LOGIC_QUEUE_ERROR_BIT = 1 << 2;

    private static final int WRITE_INDEX_FILE_ERROR_BIT = 1 << 3;

    private static final int DISK_FULL_BIT = 1 << 4;

    private volatile int flagBits = 0;

    public void makeIndexFileError() {
        this.flagBits = this.flagBits | WRITE_INDEX_FILE_ERROR_BIT;
    }


    public void makeLogicQueueError() {
        this.flagBits = this.flagBits | WRITE_LOGIC_QUEUE_ERROR_BIT;
    }

    public boolean isCQWriteable() {
        return (this.flagBits & (NOT_WRITEABLE_BIT | WRITE_LOGIC_QUEUE_ERROR_BIT | WRITE_INDEX_FILE_ERROR_BIT)) == 0;
    }

    public boolean isReadable() {
        return (this.flagBits & NOT_READABLE_BIT) == 0;
    }


    public boolean isWriteable() {
        return (this.flagBits & (NOT_WRITEABLE_BIT | WRITE_LOGIC_QUEUE_ERROR_BIT | DISK_FULL_BIT | WRITE_INDEX_FILE_ERROR_BIT)) == 0;
    }

    public boolean getAndMakeDiskFull() {
        boolean result = !((this.flagBits & DISK_FULL_BIT) == DISK_FULL_BIT);
        this.flagBits |= DISK_FULL_BIT;
        return result;
    }

    public boolean getAndMakeDiskOK() {
        boolean result = !((this.flagBits & DISK_FULL_BIT) == DISK_FULL_BIT);
        this.flagBits &= ~DISK_FULL_BIT;
        return result;
    }
}
