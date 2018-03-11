package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.ServiceThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class PullMessageService extends ServiceThread {

    private final ClientFactory clientFactory;

    private final LinkedBlockingQueue<PullRequest> pullRequestQueue = new LinkedBlockingQueue<>();

    private final ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(r -> new Thread(r, "PullMessageServiceScheduledThread"));

    @Override
    public String getServiceName() {
        return PullMessageService.class.getSimpleName();
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                PullRequest pullRequest = this.pullRequestQueue.take();
                if (pullRequest != null) {
                    this.pullMessage(pullRequest);
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                log.error("Pull Message Service Run Method exception", e);
            }
        }

        log.info(this.getServiceName() + " service end");
    }


    private void pullMessage(final PullRequest pullRequest) {
        final ConsumerInner consumer = this.clientFactory.selectConsumer(pullRequest.getConsumerGroup());
        if (consumer != null) {
            ConsumerPushDelegate delegate = (ConsumerPushDelegate) consumer;
            delegate.pullMessage(pullRequest);
        } else {
            log.warn("No matched consumer for the PullRequest {}, drop it", pullRequest);
        }
    }

    public void executePullRequestLater(final PullRequest pullRequest, final long timeDelay) {
        this.scheduledExecutorService.schedule(() -> PullMessageService.this.executePullRequestImmediately(pullRequest), timeDelay, TimeUnit.MILLISECONDS);
    }

    public void executePullRequestImmediately(final PullRequest pullRequest) {
        try {
            this.pullRequestQueue.put(pullRequest);
        } catch (InterruptedException e) {
            log.error("executePullRequestImmediately pullRequestQueue.put", e);
        }
    }

    public void executeTaskLater(final Runnable r, final long timeDelay) {
        this.scheduledExecutorService.schedule(r, timeDelay, TimeUnit.MILLISECONDS);
    }

}
