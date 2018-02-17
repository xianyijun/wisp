package cn.xianyijun.wisp.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class ReleaseOnlyOnceSemaphore {
    private final AtomicBoolean released = new AtomicBoolean(false);
    private final Semaphore semaphore;

    public void release() {
        if (this.semaphore != null) {
            if (this.released.compareAndSet(false, true)) {
                this.semaphore.release();
            }
        }
    }
}
