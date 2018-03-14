package cn.xianyijun.wisp.store.ha;

import cn.xianyijun.wisp.common.ServiceThread;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.NonNull;
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
public class ReadSocketService extends ServiceThread {
    private static final int READ_MAX_BUFFER_SIZE = 1024 * 1024;

    @NonNull
    private final HAConnection haConnection;
    private final Selector selector;
    private final SocketChannel socketChannel;
    private final ByteBuffer byteBufferRead = ByteBuffer.allocate(READ_MAX_BUFFER_SIZE);
    private int processPosition = 0;
    private volatile long lastReadTimestamp = System.currentTimeMillis();

    public ReadSocketService(final HAConnection haConnection) throws IOException {
        this.haConnection = haConnection;
        this.selector = RemotingUtils.openSelector();
        this.socketChannel = haConnection.getSocketChannel();
        this.socketChannel.register(this.selector, SelectionKey.OP_READ);
        this.thread.setDaemon(true);
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.selector.select(1000);
                boolean ok = this.processReadEvent();
                if (!ok) {
                    log.error("processReadEvent error");
                    break;
                }

                long interval = this.haConnection.getHaService().getDefaultMessageStore().getSystemClock().now() - this.lastReadTimestamp;
                if (interval > this.haConnection.getHaService().getDefaultMessageStore().getMessageStoreConfig().getHaHousekeepingInterval()) {
                    log.warn("ha housekeeping, found this connection[" + this.haConnection.getClientAddr() + "] expired, " + interval);
                    break;
                }
            } catch (Exception e) {
                log.error(this.getServiceName() + " service has exception.", e);
                break;
            }
        }

        this.makeStop();

        this.haConnection.getWriteSocketService().makeStop();

        this.haConnection.getHaService().removeConnection(this.haConnection);

        this.haConnection.getHaService().getConnectionCount().decrementAndGet();

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

    @Override
    public String getServiceName() {
        return ReadSocketService.class.getSimpleName();
    }

    private boolean processReadEvent() {
        int readSizeZeroTimes = 0;

        if (!this.byteBufferRead.hasRemaining()) {
            this.byteBufferRead.flip();
            this.processPosition = 0;
        }

        while (this.byteBufferRead.hasRemaining()) {
            try {
                int readSize = this.socketChannel.read(this.byteBufferRead);
                if (readSize > 0) {
                    readSizeZeroTimes = 0;
                    this.lastReadTimestamp = this.haConnection.getHaService().getDefaultMessageStore().getSystemClock().now();
                    if ((this.byteBufferRead.position() - this.processPosition) >= 8) {
                        int pos = this.byteBufferRead.position() - (this.byteBufferRead.position() % 8);
                        long readOffset = this.byteBufferRead.getLong(pos - 8);
                        this.processPosition = pos;

                        this.haConnection.setSlaveAckOffset(readOffset);
                        if (this.haConnection.getSlaveRequestOffset() < 0) {
                            this.haConnection.setSlaveRequestOffset(readOffset);
                            log.info("slave[" + this.haConnection.getClientAddr() + "] request offset " + readOffset);
                        }

                        this.haConnection.getHaService().notifyTransferSome(this.haConnection.getSlaveAckOffset());
                    }
                } else if (readSize == 0) {
                    if (++readSizeZeroTimes >= 3) {
                        break;
                    }
                } else {
                    log.error("read socket[" + this.haConnection.getClientAddr() + "] < 0");
                    return false;
                }
            } catch (IOException e) {
                log.error("processReadEvent exception", e);
                return false;
            }
        }

        return true;
    }
}

