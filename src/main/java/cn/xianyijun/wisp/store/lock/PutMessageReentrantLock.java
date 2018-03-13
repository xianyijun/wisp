package cn.xianyijun.wisp.store.lock;


import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xianyijun
 */
public class PutMessageReentrantLock implements PutMessageLock {
    private ReentrantLock putMessageNormalLock = new ReentrantLock();

    @Override
    public void lock() {
        putMessageNormalLock.lock();
    }

    @Override
    public void unlock() {
        putMessageNormalLock.unlock();
    }
}
