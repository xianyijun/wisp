package cn.xianyijun.wisp.store;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@NoArgsConstructor
@Slf4j
@Getter
public class MappedFile extends ReferenceResource {
    public static final int OS_PAGE_SIZE = 1024 * 4;
    private static final AtomicLong TOTAL_MAPPED_VIRTUAL_MEMORY = new AtomicLong(0);
    private static final AtomicInteger TOTAL_MAPPED_FILES = new AtomicInteger(0);
    protected final AtomicInteger wrotePosition = new AtomicInteger(0);
    protected final AtomicInteger committedPosition = new AtomicInteger(0);
    private final AtomicInteger flushedPosition = new AtomicInteger(0);
    private TransientStorePool transientStorePool = null;
    private ByteBuffer writeBuffer = null;
    private String fileName;
    private long fileFromOffset;
    private File file;
    private MappedByteBuffer mappedByteBuffer;
    private volatile long storeTimestamp = 0;
    @Setter
    private boolean firstCreateInQueue = false;


    private int fileSize;
    private FileChannel fileChannel;

    public MappedFile(final String fileName, final int fileSize) throws IOException {
        init(fileName, fileSize);
    }

    public MappedFile(final String fileName, final int fileSize,
                      final TransientStorePool transientStorePool) throws IOException {
        init(fileName, fileSize, transientStorePool);
    }

    public static void ensureDirOK(final String dirName) {
        if (dirName != null) {
            File f = new File(dirName);
            if (!f.exists()) {
                boolean result = f.mkdirs();
                log.info(dirName + " mkdir " + (result ? "OK" : "Failed"));
            }
        }
    }

    private void init(final String fileName, final int fileSize,
                      final TransientStorePool transientStorePool) throws IOException {
        init(fileName, fileSize);
        this.writeBuffer = transientStorePool.borrowBuffer();
        this.transientStorePool = transientStorePool;
    }

    private void init(final String fileName, final int fileSize) throws IOException {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = new File(fileName);
        this.fileFromOffset = Long.parseLong(this.file.getName());
        boolean ok = false;

        ensureDirOK(this.file.getParent());

        try {
            this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            TOTAL_MAPPED_VIRTUAL_MEMORY.addAndGet(fileSize);
            TOTAL_MAPPED_FILES.incrementAndGet();
            ok = true;
        } catch (FileNotFoundException e) {
            log.error("create file channel " + this.fileName + " Failed. ", e);
            throw e;
        } catch (IOException e) {
            log.error("map file " + this.fileName + " Failed. ", e);
            throw e;
        } finally {
            if (!ok && this.fileChannel != null) {
                this.fileChannel.close();
            }
        }
    }

    public int flush(final int flushLeastPages) {
        if (this.isAbleToFlush(flushLeastPages)) {
            if (this.hold()) {
                int value = getReadPosition();

                try {
                    if (writeBuffer != null || this.fileChannel.position() != 0) {
                        this.fileChannel.force(false);
                    } else {
                        this.mappedByteBuffer.force();
                    }
                } catch (Throwable e) {
                    log.error("Error occurred when force data to disk.", e);
                }

                this.flushedPosition.set(value);
                this.release();
            } else {
                log.warn("in flush, hold failed, flush offset = " + this.flushedPosition.get());
                this.flushedPosition.set(getReadPosition());
            }
        }
        return this.getFlushedPosition();
    }

    public boolean destroy(final long intervalForcibly) {
        this.shutdown(intervalForcibly);

        if (this.isCleanupOver()) {
            try {
                this.fileChannel.close();
                log.info("close file channel " + this.fileName + " OK");

                boolean result = this.file.delete();
                log.info("delete file[REF: {} ]  {} {} W: {}  M: {}", this.getRefCount(), this.fileName, result ? " OK " : " Failed", this.getWrotePosition(), this.getFlushedPosition());
            } catch (Exception e) {
                log.warn("close file channel " + this.fileName + " Failed. ", e);
            }

            return true;
        } else {
            log.warn("destroy mapped file[REF:" + this.getRefCount() + "] " + this.fileName
                    + " Failed. cleanupOver: " + this.cleanupOver);
        }

        return false;
    }

    public ByteBuffer sliceByteBuffer() {
        return this.mappedByteBuffer.slice();
    }

    public int getReadPosition() {
        return this.writeBuffer == null ? this.wrotePosition.get() : this.committedPosition.get();
    }

    public int getWrotePosition() {
        return wrotePosition.get();
    }

    public void setWrotePosition(int pos) {
        this.wrotePosition.set(pos);
    }

    public void setFlushedPosition(int pos) {
        this.flushedPosition.set(pos);
    }


    public void setCommittedPosition(int pos) {
        this.committedPosition.set(pos);
    }

    public boolean isFull() {
        return this.fileSize == this.wrotePosition.get();
    }

    public boolean appendMessage(final byte[] data) {
        int currentPos = this.wrotePosition.get();

        if ((currentPos + data.length) <= this.fileSize) {
            try {
                this.fileChannel.position(currentPos);
                this.fileChannel.write(ByteBuffer.wrap(data));
            } catch (Throwable e) {
                log.error("Error occurred when append message to mappedFile.", e);
            }
            this.wrotePosition.addAndGet(data.length);
            return true;
        }

        return false;
    }

    public boolean appendMessage(final byte[] data, final int offset, final int length) {
        int currentPos = this.wrotePosition.get();

        if ((currentPos + length) <= this.fileSize) {
            try {
                this.fileChannel.position(currentPos);
                this.fileChannel.write(ByteBuffer.wrap(data, offset, length));
            } catch (Throwable e) {
                log.error("Error occurred when append message to mappedFile.", e);
            }
            this.wrotePosition.addAndGet(length);
            return true;
        }
        return false;
    }


    public SelectMappedBufferResult selectMappedBuffer(int pos) {
        int readPosition = getReadPosition();
        if (pos < readPosition && pos >= 0) {
            if (this.hold()) {
                ByteBuffer byteBuffer = this.mappedByteBuffer.slice();
                byteBuffer.position(pos);
                int size = readPosition - pos;
                ByteBuffer byteBufferNew = byteBuffer.slice();
                byteBufferNew.limit(size);
                return new SelectMappedBufferResult(this.fileFromOffset + pos, byteBufferNew, size, this);
            }
        }
        return null;
    }


    public int getFlushedPosition() {
        return flushedPosition.get();
    }


    private boolean isAbleToFlush(final int flushLeastPages) {
        int flush = this.flushedPosition.get();
        int write = getReadPosition();

        if (this.isFull()) {
            return true;
        }

        if (flushLeastPages > 0) {
            return ((write / OS_PAGE_SIZE) - (flush / OS_PAGE_SIZE)) >= flushLeastPages;
        }

        return write > flush;
    }

    public int commit(final int commitLeastPages) {
        if (writeBuffer == null) {
            //no need to commit data to file channel, so just regard wrotePosition as committedPosition.
            return this.wrotePosition.get();
        }
        if (this.isAbleToCommit(commitLeastPages)) {
            if (this.hold()) {
                commit0(commitLeastPages);
                this.release();
            } else {
                log.warn("in commit, hold failed, commit offset = " + this.committedPosition.get());
            }
        }

        // All dirty data has been committed to FileChannel.
        if (writeBuffer != null && this.transientStorePool != null && this.fileSize == this.committedPosition.get()) {
            this.transientStorePool.returnBuffer(writeBuffer);
            this.writeBuffer = null;
        }

        return this.committedPosition.get();
    }

    protected void commit0(final int commitLeastPages) {
        int writePos = this.wrotePosition.get();
        int lastCommittedPosition = this.committedPosition.get();

        if (writePos - this.committedPosition.get() > 0) {
            try {
                ByteBuffer byteBuffer = writeBuffer.slice();
                byteBuffer.position(lastCommittedPosition);
                byteBuffer.limit(writePos);
                this.fileChannel.position(lastCommittedPosition);
                this.fileChannel.write(byteBuffer);
                this.committedPosition.set(writePos);
            } catch (Throwable e) {
                log.error("Error occurred when commit data to FileChannel.", e);
            }
        }
    }

    protected boolean isAbleToCommit(final int commitLeastPages) {
        int flush = this.committedPosition.get();
        int write = this.wrotePosition.get();

        if (this.isFull()) {
            return true;
        }

        if (commitLeastPages > 0) {
            return ((write / OS_PAGE_SIZE) - (flush / OS_PAGE_SIZE)) >= commitLeastPages;
        }

        return write > flush;
    }

    @Override
    public boolean cleanup(long currentRef) {
        return false;
    }


    public static void clean(final ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect() || buffer.capacity() == 0) {
            return;
        }
        invoke(invoke(viewed(buffer), "cleaner"), "clean");
    }

    private static Object invoke(final Object target, final String methodName, final Class<?>... args) {
        return AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try {
                Method method = method(target, methodName, args);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static ByteBuffer viewed(ByteBuffer buffer) {
        String methodName = "viewedBuffer";

        Method[] methods = buffer.getClass().getMethods();
        for (Method method : methods) {
            if ("attachment".equals(method.getName())) {
                methodName = "attachment";
                break;
            }
        }

        ByteBuffer viewedBuffer = (ByteBuffer) invoke(buffer, methodName);
        if (viewedBuffer == null) {
            return buffer;
        } else {
            return viewed(viewedBuffer);
        }
    }

    private static Method method(Object target, String methodName, Class<?>[] args)
            throws NoSuchMethodException {
        try {
            return target.getClass().getMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            return target.getClass().getDeclaredMethod(methodName, args);
        }
    }
}
