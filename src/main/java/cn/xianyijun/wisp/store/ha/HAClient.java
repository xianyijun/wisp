package cn.xianyijun.wisp.store.ha;

import cn.xianyijun.wisp.common.ServiceThread;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author xianyijun
 */
@Slf4j
public class HAClient extends ServiceThread {
    private static final int READ_MAX_BUFFER_SIZE = 1024 * 1024 * 4;
    private final AtomicReference<String> masterAddress = new AtomicReference<>();
    private final ByteBuffer reportOffset = ByteBuffer.allocate(8);
    private final HAService haService;
    private SocketChannel socketChannel;
    private Selector selector;
    private long lastWriteTimestamp = System.currentTimeMillis();

    private long currentReportedOffset = 0;
    private int dispatchPosition = 0;
    private ByteBuffer byteBufferRead = ByteBuffer.allocate(READ_MAX_BUFFER_SIZE);
    private ByteBuffer byteBufferBackup = ByteBuffer.allocate(READ_MAX_BUFFER_SIZE);

    public HAClient(HAService haService) throws IOException {
        this.haService = haService;
        this.selector = RemotingUtils.openSelector();
    }

    public void updateMasterAddress(final String newAddr) {
        String currentAddr = this.masterAddress.get();
        if (currentAddr == null || !currentAddr.equals(newAddr)) {
            this.masterAddress.set(newAddr);
            log.info("update master address, OLD: " + currentAddr + " NEW: " + newAddr);
        }
    }

    private boolean isTimeToReportOffset() {
        long interval =
                haService.getDefaultMessageStore().getSystemClock().now() - this.lastWriteTimestamp;

        return interval > haService.getDefaultMessageStore().getMessageStoreConfig()
                .getHaSendHeartbeatInterval();
    }

    private boolean reportSlaveMaxOffset(final long maxOffset) {
        this.reportOffset.position(0);
        this.reportOffset.limit(8);
        this.reportOffset.putLong(maxOffset);
        this.reportOffset.position(0);
        this.reportOffset.limit(8);

        for (int i = 0; i < 3 && this.reportOffset.hasRemaining(); i++) {
            try {
                this.socketChannel.write(this.reportOffset);
            } catch (IOException e) {
                log.error(this.getServiceName()
                        + "reportSlaveMaxOffset this.socketChannel.write exception", e);
                return false;
            }
        }

        return !this.reportOffset.hasRemaining();
    }

    private void reallocateByteBuffer() {
        int remain = READ_MAX_BUFFER_SIZE - this.dispatchPosition;
        if (remain > 0) {
            this.byteBufferRead.position(this.dispatchPosition);

            this.byteBufferBackup.position(0);
            this.byteBufferBackup.limit(READ_MAX_BUFFER_SIZE);
            this.byteBufferBackup.put(this.byteBufferRead);
        }

        this.swapByteBuffer();

        this.byteBufferRead.position(remain);
        this.byteBufferRead.limit(READ_MAX_BUFFER_SIZE);
        this.dispatchPosition = 0;
    }

    private void swapByteBuffer() {
        ByteBuffer tmp = this.byteBufferRead;
        this.byteBufferRead = this.byteBufferBackup;
        this.byteBufferBackup = tmp;
    }

    private boolean processReadEvent() {
        int readSizeZeroTimes = 0;
        while (this.byteBufferRead.hasRemaining()) {
            try {
                int readSize = this.socketChannel.read(this.byteBufferRead);
                if (readSize > 0) {
                    lastWriteTimestamp = haService.getDefaultMessageStore().getSystemClock().now();
                    readSizeZeroTimes = 0;
                    boolean result = this.dispatchReadRequest();
                    if (!result) {
                        log.error("HAClient, dispatchReadRequest error");
                        return false;
                    }
                } else if (readSize == 0) {
                    if (++readSizeZeroTimes >= 3) {
                        break;
                    }
                } else {
                    log.info("HAClient, processReadEvent read socket < 0");
                    return false;
                }
            } catch (IOException e) {
                log.info("HAClient, processReadEvent read socket exception", e);
                return false;
            }
        }

        return true;
    }

    private boolean dispatchReadRequest() {
        final int msgHeaderSize = 8 + 4; // phyoffset + size
        int readSocketPos = this.byteBufferRead.position();

        while (true) {
            int diff = this.byteBufferRead.position() - this.dispatchPosition;
            if (diff >= msgHeaderSize) {
                long masterPhyOffset = this.byteBufferRead.getLong(this.dispatchPosition);
                int bodySize = this.byteBufferRead.getInt(this.dispatchPosition + 8);

                long slavePhyOffset = haService.getDefaultMessageStore().getMaxPhyOffset();

                if (slavePhyOffset != 0) {
                    if (slavePhyOffset != masterPhyOffset) {
                        log.error("master pushed offset not equal the max phy offset in slave, SLAVE: "
                                + slavePhyOffset + " MASTER: " + masterPhyOffset);
                        return false;
                    }
                }

                if (diff >= (msgHeaderSize + bodySize)) {
                    byte[] bodyData = new byte[bodySize];
                    this.byteBufferRead.position(this.dispatchPosition + msgHeaderSize);
                    this.byteBufferRead.get(bodyData);

                    haService.getDefaultMessageStore().appendToCommitLog(masterPhyOffset, bodyData);

                    this.byteBufferRead.position(readSocketPos);
                    this.dispatchPosition += msgHeaderSize + bodySize;

                    if (reportSlaveMaxOffsetPlus()) {
                        return false;
                    }

                    continue;
                }
            }

            if (!this.byteBufferRead.hasRemaining()) {
                this.reallocateByteBuffer();
            }

            break;
        }

        return true;
    }

    private boolean reportSlaveMaxOffsetPlus() {
        boolean result = true;
        long currentPhyOffset = haService.getDefaultMessageStore().getMaxPhyOffset();
        if (currentPhyOffset > this.currentReportedOffset) {
            this.currentReportedOffset = currentPhyOffset;
            result = this.reportSlaveMaxOffset(this.currentReportedOffset);
            if (!result) {
                this.closeMaster();
                log.error("HAClient, reportSlaveMaxOffset error, " + this.currentReportedOffset);
            }
        }

        return !result;
    }

    private boolean connectMaster() throws ClosedChannelException {
        if (null == socketChannel) {
            String addr = this.masterAddress.get();
            if (addr != null) {

                SocketAddress socketAddress = RemotingUtils.string2SocketAddress(addr);
                this.socketChannel = RemotingUtils.connect(socketAddress);
                if (this.socketChannel != null) {
                    this.socketChannel.register(this.selector, SelectionKey.OP_READ);
                }
            }

            this.currentReportedOffset = haService.getDefaultMessageStore().getMaxPhyOffset();

            this.lastWriteTimestamp = System.currentTimeMillis();
        }

        return this.socketChannel != null;
    }

    private void closeMaster() {
        if (null != this.socketChannel) {
            try {

                SelectionKey sk = this.socketChannel.keyFor(this.selector);
                if (sk != null) {
                    sk.cancel();
                }

                this.socketChannel.close();

                this.socketChannel = null;
            } catch (IOException e) {
                log.warn("closeMaster exception. ", e);
            }

            this.lastWriteTimestamp = 0;
            this.dispatchPosition = 0;

            this.byteBufferBackup.position(0);
            this.byteBufferBackup.limit(READ_MAX_BUFFER_SIZE);

            this.byteBufferRead.position(0);
            this.byteBufferRead.limit(READ_MAX_BUFFER_SIZE);
        }
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                if (this.connectMaster()) {

                    if (this.isTimeToReportOffset()) {
                        boolean result = this.reportSlaveMaxOffset(this.currentReportedOffset);
                        if (!result) {
                            this.closeMaster();
                        }
                    }

                    this.selector.select(1000);

                    boolean ok = this.processReadEvent();
                    if (!ok) {
                        this.closeMaster();
                    }

                    if (reportSlaveMaxOffsetPlus()) {
                        continue;
                    }

                    long interval = haService.getDefaultMessageStore().getSystemClock().now()
                            - this.lastWriteTimestamp;
                    if (interval > haService.getDefaultMessageStore().getMessageStoreConfig()
                            .getHaHousekeepingInterval()) {
                        log.warn("HAClient, housekeeping, found this connection[" + this.masterAddress
                                + "] expired, " + interval);
                        this.closeMaster();
                        log.warn("HAClient, master not response some time, so close connection");
                    }
                } else {
                    this.waitForRunning(1000 * 5);
                }
            } catch (Exception e) {
                log.warn(this.getServiceName() + " service has exception. ", e);
                this.waitForRunning(1000 * 5);
            }
        }

        log.info(this.getServiceName() + " service end");
    }


    @Override
    public String getServiceName() {
        return HAClient.class.getSimpleName();
    }
}
