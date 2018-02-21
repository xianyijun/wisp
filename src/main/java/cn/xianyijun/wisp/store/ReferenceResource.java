package cn.xianyijun.wisp.store;

import java.util.concurrent.atomic.AtomicLong;

public abstract class ReferenceResource {
    protected final AtomicLong refCount = new AtomicLong(1);

    protected volatile boolean cleanupOver = false;

    public void release() {
        long value = this.refCount.decrementAndGet();
        if (value > 0) {
            return;
        }

        synchronized (this) {
            this.cleanupOver = this.cleanup(value);
        }
    }

    /**
     * Cleanup boolean.
     *
     * @param currentRef the current ref
     * @return the boolean
     */
    public abstract boolean cleanup(final long currentRef);

}
