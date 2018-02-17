package cn.xianyijun.wisp.remoting.netty;

import lombok.Getter;
import lombok.Setter;

/**
 * @author xianyijun
 */
@Setter
@Getter
public class NettyServerConfig implements Cloneable {
    private int listenPort = 8888;

    private int serverOneWaySemaphoreValue = 256;
    private int serverAsyncSemaphoreValue = 64;

    private int serverCallbackExecutorThreads = 0;
    private int serverWorkerThreads = 8;
    private int serverSelectorThreads = 3;

    private boolean useEPollNativeSelector = false;

    private int serverSocketSndBufSize = NettySystemConfig.socketSndBufSize;
    private int serverSocketRcvBufSize = NettySystemConfig.socketRcvBufSize;
    private boolean serverPooledByteBufAllocatorEnable = true;

    private int serverChannelMaxIdleTimeSeconds = 120;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
