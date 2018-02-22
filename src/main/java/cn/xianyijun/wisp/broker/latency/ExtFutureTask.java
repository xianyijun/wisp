package cn.xianyijun.wisp.broker.latency;

import lombok.Getter;

import java.util.concurrent.FutureTask;

/**
 * @author xianyijun
 */
@Getter
public class ExtFutureTask<V> extends FutureTask<V> {
    private final Runnable runnable;

    public ExtFutureTask(final Runnable runnable, final V result) {
        super(runnable, result);
        this.runnable = runnable;
    }
}

