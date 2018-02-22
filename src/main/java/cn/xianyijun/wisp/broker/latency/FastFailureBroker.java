package cn.xianyijun.wisp.broker.latency;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.common.WispThreadFactory;
import cn.xianyijun.wisp.remoting.netty.RequestTask;
import cn.xianyijun.wisp.remoting.protocol.RemotingSysResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class FastFailureBroker {
    private final BrokerController brokerController;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new WispThreadFactory(
            "BrokerFastFailureScheduledThread"));


    public void start() {
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (brokerController.getBrokerConfig().isBrokerFastFailureEnable()) {
                cleanExpiredRequest();
            }
        }, 1000, 10, TimeUnit.MILLISECONDS);
    }

    private void cleanExpiredRequest() {
        while (this.brokerController.getMessageStore().isOSPageCacheBusy()) {
            try {
                if (!this.brokerController.getSendThreadPoolQueue().isEmpty()) {
                    final Runnable runnable = this.brokerController.getSendThreadPoolQueue().poll(0, TimeUnit.SECONDS);
                    if (null == runnable) {
                        break;
                    }

                    final RequestTask rt = castRunnable(runnable);
                    rt.returnResponse(RemotingSysResponseCode.SYSTEM_BUSY, String.format("[PCBUSY_CLEAN_QUEUE]broker busy, start flow control for a while, period in queue: %sms, size of queue: %d", System.currentTimeMillis() - rt.getCreateTimestamp(), this.brokerController.getSendThreadPoolQueue().size()));
                } else {
                    break;
                }
            } catch (Throwable ignored) {
            }
        }

        cleanExpiredRequestInQueue(this.brokerController.getSendThreadPoolQueue(),
                this.brokerController.getBrokerConfig().getWaitTimeMillsInSendQueue());

        cleanExpiredRequestInQueue(this.brokerController.getPullThreadPoolQueue(),
                this.brokerController.getBrokerConfig().getWaitTimeMillsInPullQueue());
    }

    private void cleanExpiredRequestInQueue(final BlockingQueue<Runnable> blockingQueue, final long maxWaitTimeMillsInQueue) {
        while (true) {
            try {
                if (!blockingQueue.isEmpty()) {
                    final Runnable runnable = blockingQueue.peek();
                    if (null == runnable) {
                        break;
                    }
                    final RequestTask rt = castRunnable(runnable);
                    if (rt == null || rt.isStopRun()) {
                        break;
                    }

                    final long behind = System.currentTimeMillis() - rt.getCreateTimestamp();
                    if (behind >= maxWaitTimeMillsInQueue) {
                        if (blockingQueue.remove(runnable)) {
                            rt.setStopRun(true);
                            rt.returnResponse(RemotingSysResponseCode.SYSTEM_BUSY, String.format("[TIMEOUT_CLEAN_QUEUE]broker busy, start flow control for a while, period in queue: %sms, size of queue: %d", behind, blockingQueue.size()));
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Throwable ignored) {
            }
        }
    }


    public static RequestTask castRunnable(final Runnable runnable) {
        try {
            ExtFutureTask object = (ExtFutureTask) runnable;
            return (RequestTask) object.getRunnable();
        } catch (Throwable e) {
            log.error(String.format("castRunnable exception, %s", runnable.getClass().getName()), e);
        }

        return null;
    }

    public void shutdown() {
        this.scheduledExecutorService.shutdown();
    }

}
