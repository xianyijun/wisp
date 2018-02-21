package cn.xianyijun.wisp.broker.latency;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.remoting.netty.RequestTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class FastFailureBroker {
    private final BrokerController brokerController;

    public static RequestTask castRunnable(final Runnable runnable) {
        try {
            FutureTaskExt object = (FutureTaskExt) runnable;
            return (RequestTask) object.getRunnable();
        } catch (Throwable e) {
            log.error(String.format("castRunnable exception, %s", runnable.getClass().getName()), e);
        }

        return null;
    }
}
