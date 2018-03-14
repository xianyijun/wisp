package cn.xianyijun.wisp.store.ha;

import cn.xianyijun.wisp.common.ServiceThread;
import cn.xianyijun.wisp.store.io.CommitLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
public class GroupTransferService extends ServiceThread {

    private final HAService haService;
    private final WaitNotifyObject notifyTransferObject = new WaitNotifyObject();
    private volatile List<CommitLog.GroupCommitRequest> requestsWrite = new ArrayList<>();
    private volatile List<CommitLog.GroupCommitRequest> requestsRead = new ArrayList<>();

    synchronized void putRequest(final CommitLog.GroupCommitRequest request) {
        synchronized (this.requestsWrite) {
            this.requestsWrite.add(request);
        }
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown();
        }
    }

    public void notifyTransferSome() {
        this.notifyTransferObject.wakeup();
    }

    private void swapRequests() {
        List<CommitLog.GroupCommitRequest> tmp = this.requestsWrite;
        this.requestsWrite = this.requestsRead;
        this.requestsRead = tmp;
    }

    private void doWaitTransfer() {
        synchronized (this.requestsRead) {
            if (this.requestsRead.isEmpty()) {
                return;
            }
            for (CommitLog.GroupCommitRequest req : this.requestsRead) {
                boolean transferOK = this.haService.getPushSlaveMaxOffset().get() >= req.getNextOffset();
                for (int i = 0; !transferOK && i < 5; i++) {
                    this.notifyTransferObject.waitForRunning(1000);
                    transferOK = this.haService.getPushSlaveMaxOffset().get() >= req.getNextOffset();
                }

                if (!transferOK) {
                    log.warn("transfer message to slave timeout, " + req.getNextOffset());
                }

                req.wakeupCustomer(transferOK);
            }

            this.requestsRead.clear();
        }
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.waitForRunning(10);
                this.doWaitTransfer();
            } catch (Exception e) {
                log.warn(this.getServiceName() + " service has exception. ", e);
            }
        }

        log.info(this.getServiceName() + " service end");
    }

    @Override
    protected void onWaitEnd() {
        this.swapRequests();
    }

    @Override
    public String getServiceName() {
        return GroupTransferService.class.getSimpleName();
    }
}

