package cn.xianyijun.wisp.store;

/**
 * The interface Put message lock.
 */
public interface PutMessageLock {
    /**
     * Lock.
     */
    void lock();

    /**
     * Unlock.
     */
    void unlock();
}
