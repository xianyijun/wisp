package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.remoting.netty.ResponseFuture;

/**
 * The interface Invoke callback.
 */
public interface InvokeCallback {
    /**
     * Operation complete.
     *
     * @param responseFuture the response future
     */
    void operationComplete(final ResponseFuture responseFuture);
}