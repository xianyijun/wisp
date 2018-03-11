package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.common.ServiceThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReBalanceService extends ServiceThread {

    private static long waitInterval =
            Long.parseLong(System.getProperty(
                    "wisp.client.rebalance.waitInterval", "20000"));

    private final ClientFactory clientFactory;

    @Override
    public String getServiceName() {
        return ReBalanceService.class.getSimpleName();
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            this.waitForRunning(waitInterval);
            this.clientFactory.doReBalance();
        }

        log.info(this.getServiceName() + " service end");
    }
}
