package cn.xianyijun.wisp.store.index;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
@Slf4j
public class IndexHeader {
    public static final int INDEX_HEADER_SIZE = 40;
    private static int beginTimestampIndex = 0;
    private static int endTimestampIndex = 8;
    private static int beginPhyOffsetIndex = 16;
    private static int endPhyOffsetIndex = 24;
    private static int hashSlotCountIndex = 32;
    private static int indexCountIndex = 36;
    private final ByteBuffer byteBuffer;
    private AtomicLong beginTimestamp = new AtomicLong(0);
    private AtomicLong endTimestamp = new AtomicLong(0);
    private AtomicLong beginPhyOffset = new AtomicLong(0);
    private AtomicLong endPhyOffset = new AtomicLong(0);
    private AtomicInteger hashSlotCount = new AtomicInteger(0);

    private AtomicInteger indexCount = new AtomicInteger(1);


    public void load() {
        this.beginTimestamp.set(byteBuffer.getLong(beginTimestampIndex));
        this.endTimestamp.set(byteBuffer.getLong(endTimestampIndex));
        this.beginPhyOffset.set(byteBuffer.getLong(beginPhyOffsetIndex));
        this.endPhyOffset.set(byteBuffer.getLong(endPhyOffsetIndex));

        this.hashSlotCount.set(byteBuffer.getInt(hashSlotCountIndex));
        this.indexCount.set(byteBuffer.getInt(indexCountIndex));

        if (this.indexCount.get() <= 0) {
            this.indexCount.set(1);
        }
    }

    public long getBeginTimestamp() {
        return beginTimestamp.get();
    }

    public void setBeginTimestamp(long beginTimestamp) {
        this.beginTimestamp.set(beginTimestamp);
        this.byteBuffer.putLong(beginTimestampIndex, beginTimestamp);
    }

    public long getEndTimestamp() {
        return endTimestamp.get();
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp.set(endTimestamp);
        this.byteBuffer.putLong(endTimestampIndex, endTimestamp);
    }

    public long getBeginPhyOffset() {
        return beginPhyOffset.get();
    }

    public void setBeginPhyOffset(long beginPhyOffset) {
        this.beginPhyOffset.set(beginPhyOffset);
        this.byteBuffer.putLong(beginPhyOffsetIndex, beginPhyOffset);
    }

    public long getEndPhyOffset() {
        return endPhyOffset.get();
    }

    public void setEndPhyOffset(long endPhyOffset) {
        this.endPhyOffset.set(endPhyOffset);
        this.byteBuffer.putLong(endPhyOffsetIndex, endPhyOffset);
    }

    public AtomicInteger getHashSlotCount() {
        return hashSlotCount;
    }

    public void incHashSlotCount() {
        int value = this.hashSlotCount.incrementAndGet();
        this.byteBuffer.putInt(hashSlotCountIndex, value);
    }

    public int getIndexCount() {
        return indexCount.get();
    }

    public void incIndexCount() {
        int value = this.indexCount.incrementAndGet();
        this.byteBuffer.putInt(indexCountIndex, value);
    }


    public void updateByteBuffer() {
        this.byteBuffer.putLong(beginTimestampIndex, this.beginTimestamp.get());
        this.byteBuffer.putLong(endTimestampIndex, this.endTimestamp.get());
        this.byteBuffer.putLong(beginPhyOffsetIndex, this.beginPhyOffset.get());
        this.byteBuffer.putLong(endPhyOffsetIndex, this.endPhyOffset.get());
        this.byteBuffer.putInt(hashSlotCountIndex, this.hashSlotCount.get());
        this.byteBuffer.putInt(indexCountIndex, this.indexCount.get());
    }
}