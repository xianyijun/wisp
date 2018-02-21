package cn.xianyijun.wisp.broker.latency;

import lombok.Getter;

import java.util.concurrent.FutureTask;

@Getter
public class FutureTaskExt<V> extends FutureTask<V> {
    private final Runnable runnable;

    public FutureTaskExt(final Runnable runnable, final V result) {
        super(runnable, result);
        this.runnable = runnable;
    }
}

