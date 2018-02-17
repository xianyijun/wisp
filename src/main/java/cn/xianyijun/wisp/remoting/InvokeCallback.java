package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.remoting.netty.ResponseFuture;

public interface InvokeCallback {
    void operationComplete(final ResponseFuture responseFuture);
}