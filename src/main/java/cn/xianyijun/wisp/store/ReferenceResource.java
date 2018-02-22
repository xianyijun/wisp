package cn.xianyijun.wisp.store;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public abstract class ReferenceResource {
    protected final AtomicLong refCount = new AtomicLong(1);

    protected volatile boolean cleanupOver = false;

    protected volatile boolean available = true;

    private volatile long firstShutdownTimestamp = 0;

    public void release() {
        long value = this.refCount.decrementAndGet();
        if (value > 0) {
            return;
        }

        synchronized (this) {
            this.cleanupOver = this.cleanup(value);
        }
    }

    public synchronized boolean hold() {
        if (this.isAvailable()) {
            if (this.refCount.getAndIncrement() > 0) {
                return true;
            } else {
                this.refCount.getAndDecrement();
            }
        }

        return false;
    }


    public void shutdown(final long intervalForcibly) {
        if (this.available) {
            this.available = false;
            this.firstShutdownTimestamp = System.currentTimeMillis();
            this.release();
        } else if (this.getRefCount() > 0) {
            if ((System.currentTimeMillis() - this.firstShutdownTimestamp) >= intervalForcibly) {
                this.refCount.set(-1000 - this.getRefCount());
                this.release();
            }
        }
    }

    public long getRefCount() {
        return this.refCount.get();
    }

    /**
     * Cleanup boolean.
     *
     * @param currentRef the current ref
     * @return the boolean
     */
    public abstract boolean cleanup(final long currentRef);

}
