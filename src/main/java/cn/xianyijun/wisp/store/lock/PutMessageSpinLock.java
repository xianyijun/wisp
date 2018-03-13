package cn.xianyijun.wisp.store.lock;

import java.util.concurrent.atomic.AtomicBoolean;

public class PutMessageSpinLock implements PutMessageLock {
    private AtomicBoolean putMessageSpinLock = new AtomicBoolean(true);

    @Override
    public void lock() {
        boolean flag;
        do {
            flag = this.putMessageSpinLock.compareAndSet(true, false);
        }
        while (!flag);
    }

    @Override
    public void unlock() {
        this.putMessageSpinLock.compareAndSet(false, true);
    }
}
