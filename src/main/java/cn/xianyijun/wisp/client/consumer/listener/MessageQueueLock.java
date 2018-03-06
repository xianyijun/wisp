package cn.xianyijun.wisp.client.consumer.listener;

import cn.xianyijun.wisp.common.message.MessageQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
public class MessageQueueLock {
    private ConcurrentMap<MessageQueue, Object> mqLockTable =
            new ConcurrentHashMap<MessageQueue, Object>();

    public Object fetchLockObject(final MessageQueue mq) {
        Object objLock = this.mqLockTable.get(mq);
        if (null == objLock) {
            objLock = new Object();
            Object prevLock = this.mqLockTable.putIfAbsent(mq, objLock);
            if (prevLock != null) {
                objLock = prevLock;
            }
        }

        return objLock;
    }
}
