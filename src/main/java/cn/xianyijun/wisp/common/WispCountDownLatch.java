package cn.xianyijun.wisp.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class WispCountDownLatch {
    private final Sync sync;


    public WispCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.sync = new Sync(count);
    }

    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void countDown() {
        sync.releaseShared(1);
    }

    public long getCount() {
        return sync.getCount();
    }

    public void reset() {
        sync.reset();
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        private final int startCount;

        Sync(int count) {
            this.startCount = count;
            setState(count);
        }

        int getCount() {
            return getState();
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
                int c = getState();
                if (c == 0) {
                    return false;
                }
                int next = c - 1;
                if (compareAndSetState(c, next)) {
                    return next == 0;
                }
            }
        }

        protected void reset() {
            setState(startCount);
        }
    }
}
