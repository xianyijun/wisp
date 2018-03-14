package cn.xianyijun.wisp.store.ha;

import cn.xianyijun.wisp.store.DefaultMessageStore;
import cn.xianyijun.wisp.store.io.CommitLog;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class HAService {
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    private final AtomicLong pushSlaveMaxOffset = new AtomicLong(0);

    private final List<HAConnection> connectionList = new LinkedList<>();

    private final AcceptSocketService acceptSocketService;

    private final GroupTransferService groupTransferService;

    private final DefaultMessageStore messageStore;

    private final WaitNotifyObject waitNotifyObject = new WaitNotifyObject();

    private final HAClient haClient;

    public HAService(final DefaultMessageStore messageStore) throws IOException {
        this.messageStore = messageStore;
        this.acceptSocketService =
                new AcceptSocketService(this,messageStore.getMessageStoreConfig().getHaListenPort());
        this.groupTransferService = new GroupTransferService(this);
        this.haClient = new HAClient(this);
    }

    public void notifyTransferSome(final long offset) {
        for (long value = this.pushSlaveMaxOffset.get(); offset > value; ) {
            boolean ok = this.pushSlaveMaxOffset.compareAndSet(value, offset);
            if (ok) {
                this.groupTransferService.notifyTransferSome();
                break;
            } else {
                value = this.pushSlaveMaxOffset.get();
            }
        }
    }

    public void start() throws Exception {
        this.acceptSocketService.beginAccept();
        this.acceptSocketService.start();
        this.groupTransferService.start();
        this.haClient.start();
    }

    public void shutdown() {
        this.haClient.shutdown();
        this.acceptSocketService.shutdown(true);
        this.destroyConnections();
        this.groupTransferService.shutdown();
    }

    public void addConnection(final HAConnection conn) {
        synchronized (this.connectionList) {
            this.connectionList.add(conn);
        }
    }

    public void destroyConnections() {
        synchronized (this.connectionList) {
            for (HAConnection c : this.connectionList) {
                c.shutdown();
            }

            this.connectionList.clear();
        }
    }


    public void removeConnection(final HAConnection conn) {
        synchronized (this.connectionList) {
            this.connectionList.remove(conn);
        }
    }

    public void updateMasterAddress(final String newAddr) {
        if (this.haClient != null) {
            this.haClient.updateMasterAddress(newAddr);
        }
    }


    public void putRequest(final CommitLog.GroupCommitRequest request) {
        this.groupTransferService.putRequest(request);
    }

    public boolean isSlaveOK(final long masterPutWhere) {
        boolean result = this.connectionCount.get() > 0;
        result = result && ((masterPutWhere - this.pushSlaveMaxOffset.get()) < this.messageStore
                        .getMessageStoreConfig().getHaSlaveFallbehindMax());
        return result;
    }

}
