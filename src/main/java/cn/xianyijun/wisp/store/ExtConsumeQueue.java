package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.store.result.SelectMappedBufferResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ExtConsumeQueue {

    public static final int END_BLANK_DATA_LENGTH = 4;
    /**
     * Addr can not exceed this value.For compatible.
     */
    public static final long MAX_ADDR = Integer.MIN_VALUE - 1L;
    public static final long MAX_REAL_OFFSET = MAX_ADDR - Long.MIN_VALUE;
    private final MappedFileQueue mappedFileQueue;
    private final String topic;
    private final int queueId;
    private final String storePath;
    private final int mappedFileSize;
    private ByteBuffer tempContainer;

    /**
     * Constructor.
     *
     * @param topic          topic
     * @param queueId        id of queue
     * @param storePath      root dir of files to store.
     * @param mappedFileSize file size
     * @param bitMapLength   bit map length.
     */
    ExtConsumeQueue(final String topic,
                    final int queueId,
                    final String storePath,
                    final int mappedFileSize,
                    final int bitMapLength) {

        this.storePath = storePath;
        this.mappedFileSize = mappedFileSize;

        this.topic = topic;
        this.queueId = queueId;

        String queueDir = this.storePath
                + File.separator + topic
                + File.separator + queueId;

        this.mappedFileQueue = new MappedFileQueue(queueDir, mappedFileSize, null);

        if (bitMapLength > 0) {
            this.tempContainer = ByteBuffer.allocate(
                    bitMapLength / Byte.SIZE
            );
        }
    }

    public static boolean isExtAddr(final long address) {
        return address <= MAX_ADDR;
    }

    public boolean flush(final int flushLeastPages) {
        return this.mappedFileQueue.flush(flushLeastPages);
    }

    public boolean load() {
        boolean result = this.mappedFileQueue.load();
        log.info("load consume queue extend" + this.topic + "-" + this.queueId + " " + (result ? "OK" : "Failed"));
        return result;
    }

    public long put(final CqExtUnit cqExtUnit) {
        final int retryTimes = 3;
        try {
            int size = cqExtUnit.calcUnitSize();
            if (size > CqExtUnit.MAX_EXT_UNIT_SIZE) {
                log.error("Size of cq ext unit is greater than {}, {}", CqExtUnit.MAX_EXT_UNIT_SIZE, cqExtUnit);
                return 1;
            }
            if (this.mappedFileQueue.getMaxOffset() + size > MAX_REAL_OFFSET) {
                log.warn("Capacity of ext is maximum!{}, {}", this.mappedFileQueue.getMaxOffset(), size);
                return 1;
            }
            // unit size maybe change.but, the same most of the time.
            if (this.tempContainer == null || this.tempContainer.capacity() < size) {
                this.tempContainer = ByteBuffer.allocate(size);
            }

            for (int i = 0; i < retryTimes; i++) {
                MappedFile mappedFile = this.mappedFileQueue.getLastMappedFile();

                if (mappedFile == null || mappedFile.isFull()) {
                    mappedFile = this.mappedFileQueue.getLastMappedFile(0);
                }

                if (mappedFile == null) {
                    log.error("Create mapped file when save consume queue extend, {}", cqExtUnit);
                    continue;
                }
                final int wrotePosition = mappedFile.getWrotePosition();
                final int blankSize = this.mappedFileSize - wrotePosition - END_BLANK_DATA_LENGTH;

                // check whether has enough space.
                if (size > blankSize) {
                    fullFillToEnd(mappedFile, wrotePosition);
                    log.info("No enough space(need:{}, has:{}) of file {}, so fill to end",
                            size, blankSize, mappedFile.getFileName());
                    continue;
                }

                if (mappedFile.appendMessage(cqExtUnit.write(this.tempContainer), 0, size)) {
                    return decorate(wrotePosition + mappedFile.getFileFromOffset());
                }
            }
        } catch (Throwable e) {
            log.error("Save consume queue extend error, " + cqExtUnit, e);
        }

        return 1;
    }

    private void fullFillToEnd(final MappedFile mappedFile, final int wrotePosition) {
        ByteBuffer mappedFileBuffer = mappedFile.sliceByteBuffer();
        mappedFileBuffer.position(wrotePosition);

        // ending.
        mappedFileBuffer.putShort((short) -1);

        mappedFile.setWrotePosition(this.mappedFileSize);
    }

    private long decorate(final long offset) {
        if (!isExtAddr(offset)) {
            return offset + Long.MIN_VALUE;
        }
        return offset;
    }

    public void recover() {
        final List<MappedFile> mappedFiles = this.mappedFileQueue.getMappedFiles();
        if (mappedFiles == null || mappedFiles.isEmpty()) {
            return;
        }

        int index = 0;

        MappedFile mappedFile = mappedFiles.get(index);
        ByteBuffer byteBuffer = mappedFile.sliceByteBuffer();
        long processOffset = mappedFile.getFileFromOffset();
        long mappedFileOffset = 0;
        CqExtUnit extUnit = new CqExtUnit();
        while (true) {
            extUnit.readBySkip(byteBuffer);

            if (extUnit.getSize() > 0) {
                mappedFileOffset += extUnit.getSize();
                continue;
            }

            index++;
            if (index < mappedFiles.size()) {
                mappedFile = mappedFiles.get(index);
                byteBuffer = mappedFile.sliceByteBuffer();
                processOffset = mappedFile.getFileFromOffset();
                mappedFileOffset = 0;
                log.info("Recover next consume queue extend file, " + mappedFile.getFileName());
                continue;
            }

            log.info("All files of consume queue extend has been recovered over, last mapped file "
                    + mappedFile.getFileName());
            break;
        }

        processOffset += mappedFileOffset;
        this.mappedFileQueue.setFlushedWhere(processOffset);
        this.mappedFileQueue.setCommittedWhere(processOffset);
        this.mappedFileQueue.truncateDirtyFiles(processOffset);
    }

    public void truncateByMaxAddress(final long maxAddress) {
        if (!isExtAddr(maxAddress)) {
            return;
        }

        log.info("Truncate consume queue ext by max {}.", maxAddress);

        CqExtUnit cqExtUnit = get(maxAddress);
        if (cqExtUnit == null) {
            log.error("[BUG] address {} of consume queue extend not found!", maxAddress);
            return;
        }

        final long realOffset = unDecorate(maxAddress);

        this.mappedFileQueue.truncateDirtyFiles(realOffset + cqExtUnit.getSize());
    }

    public CqExtUnit get(final long address) {
        CqExtUnit cqExtUnit = new CqExtUnit();
        if (get(address, cqExtUnit)) {
            return cqExtUnit;
        }
        return null;
    }

    public boolean get(final long address, final CqExtUnit cqExtUnit) {
        if (!isExtAddr(address)) {
            return false;
        }

        final int mappedFileSize = this.mappedFileSize;
        final long realOffset = unDecorate(address);

        MappedFile mappedFile = this.mappedFileQueue.findMappedFileByOffset(realOffset, realOffset == 0);
        if (mappedFile == null) {
            return false;
        }

        int pos = (int) (realOffset % mappedFileSize);

        SelectMappedBufferResult bufferResult = mappedFile.selectMappedBuffer(pos);
        if (bufferResult == null) {
            log.warn("[BUG] Consume queue extend unit({}) is not found!", realOffset);
            return false;
        }
        boolean ret;
        try {
            ret = cqExtUnit.read(bufferResult.getByteBuffer());
        } finally {
            bufferResult.release();
        }

        return ret;
    }

    public void checkSelf() {
        this.mappedFileQueue.checkSelf();
    }

    public void truncateByMinAddress(final long minAddress) {
        if (!isExtAddr(minAddress)) {
            return;
        }

        log.info("Truncate consume queue ext by min {}.", minAddress);

        List<MappedFile> willRemoveFiles = new ArrayList<MappedFile>();

        List<MappedFile> mappedFiles = this.mappedFileQueue.getMappedFiles();
        final long realOffset = unDecorate(minAddress);

        for (MappedFile file : mappedFiles) {
            long fileTailOffset = file.getFileFromOffset() + this.mappedFileSize;

            if (fileTailOffset < realOffset) {
                log.info("Destroy consume queue ext by min: file={}, fileTailOffset={}, minOffset={}", file.getFileName(),
                        fileTailOffset, realOffset);
                if (file.destroy(1000)) {
                    willRemoveFiles.add(file);
                }
            }
        }

        this.mappedFileQueue.deleteExpiredFile(willRemoveFiles);
    }

    private long unDecorate(final long address) {
        if (isExtAddr(address)) {
            return address - Long.MIN_VALUE;
        }
        return address;
    }

    public void destroy() {
        this.mappedFileQueue.destroy();
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class CqExtUnit {

        public static final short MIN_EXT_UNIT_SIZE
                = 2 // size, 32k max
                + 8 * 2 // msg time + tagCode
                + 2; // bitMapSize

        public static final int MAX_EXT_UNIT_SIZE = Short.MAX_VALUE;

        /**
         * unit size
         */
        private short size;
        /**
         * has code of tags
         */
        private long tagsCode;
        /**
         * the time to store into commit log of message
         */
        private long msgStoreTime;
        /**
         * size of bit map
         */
        private short bitMapSize;
        /**
         * filter bit map
         */
        private byte[] filterBitMap;

        public CqExtUnit(Long tagsCode, long msgStoreTime, byte[] filterBitMap) {
            this.tagsCode = tagsCode == null ? 0 : tagsCode;
            this.msgStoreTime = msgStoreTime;
            this.filterBitMap = filterBitMap;
            this.bitMapSize = (short) (filterBitMap == null ? 0 : filterBitMap.length);
            this.size = (short) (MIN_EXT_UNIT_SIZE + this.bitMapSize);
        }

        private byte[] write(final ByteBuffer container) {
            this.bitMapSize = (short) (filterBitMap == null ? 0 : filterBitMap.length);
            this.size = (short) (MIN_EXT_UNIT_SIZE + this.bitMapSize);

            ByteBuffer temp = container;

            if (temp == null || temp.capacity() < this.size) {
                temp = ByteBuffer.allocate(this.size);
            }

            temp.flip();
            temp.limit(this.size);

            temp.putShort(this.size);
            temp.putLong(this.tagsCode);
            temp.putLong(this.msgStoreTime);
            temp.putShort(this.bitMapSize);
            if (this.bitMapSize > 0) {
                temp.put(this.filterBitMap);
            }

            return temp.array();
        }

        private boolean read(final ByteBuffer buffer) {
            if (buffer.position() + 2 > buffer.limit()) {
                return false;
            }

            this.size = buffer.getShort();

            if (this.size < 1) {
                return false;
            }

            this.tagsCode = buffer.getLong();
            this.msgStoreTime = buffer.getLong();
            this.bitMapSize = buffer.getShort();

            if (this.bitMapSize < 1) {
                return true;
            }

            if (this.filterBitMap == null || this.filterBitMap.length != this.bitMapSize) {
                this.filterBitMap = new byte[bitMapSize];
            }

            buffer.get(this.filterBitMap);
            return true;
        }

        private void readBySkip(final ByteBuffer buffer) {
            ByteBuffer temp = buffer.slice();

            short tempSize = temp.getShort();
            this.size = tempSize;

            if (tempSize > 0) {
                buffer.position(buffer.position() + this.size);
            }
        }

        private int calcUnitSize() {
            return MIN_EXT_UNIT_SIZE + (filterBitMap == null ? 0 : filterBitMap.length);
        }
    }
}
