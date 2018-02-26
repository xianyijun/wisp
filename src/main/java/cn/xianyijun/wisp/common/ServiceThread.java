package cn.xianyijun.wisp.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public abstract class ServiceThread implements Runnable {

    private static final long JOIN_TIME = 90 * 1000;
    protected final Thread thread;
    protected volatile AtomicBoolean hasNotified = new AtomicBoolean(false);
    protected volatile boolean stopped = false;
    protected final WispCountDownLatch waitPoint = new WispCountDownLatch(1);

    public ServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }


    public void start() {
        this.thread.start();
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(final boolean interrupt) {
        this.stopped = true;
        log.info("shutdown thread " + this.getServiceName() + " interrupt " + interrupt);
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown();
        }

        try {
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            this.thread.join(JOIN_TIME);
            long eclipseTime = System.currentTimeMillis() - beginTime;
            log.info("join thread " + this.getServiceName() + " eclipse time(ms) " + eclipseTime + " "
                    + JOIN_TIME);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }


    protected void waitForRunning(long interval) {
        if (hasNotified.compareAndSet(true, false)) {
            this.onWaitEnd();
            return;
        }

        //entry to wait
        waitPoint.reset();

        try {
            waitPoint.await(interval, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        } finally {
            hasNotified.set(false);
            this.onWaitEnd();
        }
    }

    public void wakeup() {
        if (hasNotified.compareAndSet(false, true)) {
            waitPoint.countDown(); // notify
        }
    }

    public long getJoinTime() {
        return JOIN_TIME;
    }

    protected void onWaitEnd() {
    }

    public void makeStop() {
        this.stopped = true;
        log.info("makestop thread " + this.getServiceName());
    }
    public abstract String getServiceName();

}
