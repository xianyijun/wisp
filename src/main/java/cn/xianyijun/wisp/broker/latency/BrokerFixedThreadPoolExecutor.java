package cn.xianyijun.wisp.broker.latency;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
public class BrokerFixedThreadPoolExecutor extends ThreadPoolExecutor {

    public BrokerFixedThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime,
                                         final TimeUnit unit,
                                         final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
        return new ExtFutureTask<>(runnable, value);
    }
}
