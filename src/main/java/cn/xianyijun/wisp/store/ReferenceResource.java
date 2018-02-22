package cn.xianyijun.wisp.store;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public abstract class ReferenceResource {
    protected final AtomicLong refCount = new AtomicLong(1);

    protected volatile boolean cleanupOver = false;

    protected volatile boolean available = true;

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


    /**
     * Cleanup boolean.
     *
     * @param currentRef the current ref
     * @return the boolean
     */
    public abstract boolean cleanup(final long currentRef);

}
