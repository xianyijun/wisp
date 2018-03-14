package cn.xianyijun.wisp.store.ha;

import cn.xianyijun.wisp.common.ServiceThread;
import cn.xianyijun.wisp.store.result.SelectMappedBufferResult;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author xianyijun
 */
@Slf4j
public  class WriteSocketService extends ServiceThread {
    private final HAConnection haConnection;
    private final Selector selector;
    private final SocketChannel socketChannel;

    private final int headerSize = 8 + 4;
    private final ByteBuffer byteBufferHeader = ByteBuffer.allocate(headerSize);
    private long nextTransferFromWhere = -1;
    private SelectMappedBufferResult selectMappedBufferResult;
    private boolean lastWriteOver = true;
    private long lastWriteTimestamp = System.currentTimeMillis();

    public WriteSocketService(final HAConnection haConnection) throws IOException {
        this.haConnection = haConnection;
        this.selector = RemotingUtils.openSelector();
        this.socketChannel = haConnection.getSocketChannel();
        this.socketChannel.register(this.selector, SelectionKey.OP_WRITE);
        this.thread.setDaemon(true);
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.selector.select(1000);

                if (-1 == this.haConnection.getSlaveRequestOffset()) {
                    Thread.sleep(10);
                    continue;
                }

                if (-1 == this.nextTransferFromWhere) {
                    if (0 == this.haConnection.getSlaveRequestOffset()) {
                        long masterOffset = this.haConnection.getHaService().getDefaultMessageStore().getCommitLog().getMaxOffset();
                        masterOffset =
                                masterOffset
                                        - (masterOffset % this.haConnection.getHaService().getDefaultMessageStore().getMessageStoreConfig()
                                        .getMappedFileSizeCommitLog());

                        if (masterOffset < 0) {
                            masterOffset = 0;
                        }

                        this.nextTransferFromWhere = masterOffset;
                    } else {
                        this.nextTransferFromWhere = this.haConnection.getSlaveRequestOffset();
                    }

                    log.info("master transfer data from " + this.nextTransferFromWhere + " to slave[" + this.haConnection.getClientAddr()
                            + "], and slave request " + this.haConnection.getSlaveRequestOffset());
                }

                if (this.lastWriteOver) {

                    long interval = this.haConnection.getHaService().getDefaultMessageStore().getSystemClock().now() - this.lastWriteTimestamp;

                    if (interval > this.haConnection.getHaService().getDefaultMessageStore().getMessageStoreConfig()
                            .getHaSendHeartbeatInterval()) {

                        // Build Header
                        this.byteBufferHeader.position(0);
                        this.byteBufferHeader.limit(headerSize);
                        this.byteBufferHeader.putLong(this.nextTransferFromWhere);
                        this.byteBufferHeader.putInt(0);
                        this.byteBufferHeader.flip();

                        this.lastWriteOver = this.transferData();
                        if (!this.lastWriteOver) {
                            continue;
                        }
                    }
                } else {
                    this.lastWriteOver = this.transferData();
                    if (!this.lastWriteOver) {
                        continue;
                    }
                }

                SelectMappedBufferResult selectResult = this.haConnection.getHaService().getDefaultMessageStore().getCommitLogData(this.nextTransferFromWhere);
                if (selectResult != null) {
                    int size = selectResult.getSize();
                    if (size > this.haConnection.getHaService().getDefaultMessageStore().getMessageStoreConfig().getHaTransferBatchSize()) {
                        size = this.haConnection.getHaService().getDefaultMessageStore().getMessageStoreConfig().getHaTransferBatchSize();
                    }

                    long thisOffset = this.nextTransferFromWhere;
                    this.nextTransferFromWhere += size;

                    selectResult.getByteBuffer().limit(size);
                    this.selectMappedBufferResult = selectResult;

                    // Build Header
                    this.byteBufferHeader.position(0);
                    this.byteBufferHeader.limit(headerSize);
                    this.byteBufferHeader.putLong(thisOffset);
                    this.byteBufferHeader.putInt(size);
                    this.byteBufferHeader.flip();

                    this.lastWriteOver = this.transferData();
                } else {

                    this.haConnection.getHaService().getWaitNotifyObject().allWaitForRunning(100);
                }
            } catch (Exception e) {

                log.error(this.getServiceName() + " service has exception.", e);
                break;
            }
        }

        if (this.selectMappedBufferResult != null) {
            this.selectMappedBufferResult.release();
        }

        this.makeStop();

        this.haConnection.getReadSocketService().makeStop();

        this.haConnection.getHaService().removeConnection(this.haConnection);

        SelectionKey sk = this.socketChannel.keyFor(this.selector);
        if (sk != null) {
            sk.cancel();
        }

        try {
            this.selector.close();
            this.socketChannel.close();
        } catch (IOException e) {
            log.error("", e);
        }
        log.info(this.getServiceName() + " service end");
    }

    private boolean transferData() throws Exception {
        int writeSizeZeroTimes = 0;
        // Write Header
        while (this.byteBufferHeader.hasRemaining()) {
            int writeSize = this.socketChannel.write(this.byteBufferHeader);
            if (writeSize > 0) {
                writeSizeZeroTimes = 0;
                this.lastWriteTimestamp = this.haConnection.getHaService().getDefaultMessageStore().getSystemClock().now();
            } else if (writeSize == 0) {
                if (++writeSizeZeroTimes >= 3) {
                    break;
                }
            } else {
                throw new Exception("ha master write header error < 0");
            }
        }

        if (null == this.selectMappedBufferResult) {
            return !this.byteBufferHeader.hasRemaining();
        }

        writeSizeZeroTimes = 0;

        // Write Body
        if (!this.byteBufferHeader.hasRemaining()) {
            while (this.selectMappedBufferResult.getByteBuffer().hasRemaining()) {
                int writeSize = this.socketChannel.write(this.selectMappedBufferResult.getByteBuffer());
                if (writeSize > 0) {
                    writeSizeZeroTimes = 0;
                    this.lastWriteTimestamp = this.haConnection.getHaService().getDefaultMessageStore().getSystemClock().now();
                } else if (writeSize == 0) {
                    if (++writeSizeZeroTimes >= 3) {
                        break;
                    }
                } else {
                    throw new Exception("ha master write body error < 0");
                }
            }
        }

        boolean result = !this.byteBufferHeader.hasRemaining() && !this.selectMappedBufferResult.getByteBuffer().hasRemaining();

        if (!this.selectMappedBufferResult.getByteBuffer().hasRemaining()) {
            this.selectMappedBufferResult.release();
            this.selectMappedBufferResult = null;
        }

        return result;
    }

    @Override
    public String getServiceName() {
        return WriteSocketService.class.getSimpleName();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}