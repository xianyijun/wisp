package cn.xianyijun.wisp.remoting.netty;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
@Setter
public class NettyClientConfig {

    private int clientWorkerThreads = 4;
    private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int clientOneWaySemaphoreValue = NettySystemConfig.CLIENT_ONE_WAY_SEMAPHORE_VALUE;
    private int clientAsyncSemaphoreValue = NettySystemConfig.CLIENT_ASYNC_SEMAPHORE_VALUE;
    private int connectTimeoutMillis = 3000;
    private long channelNotActiveInterval = 1000 * 60;

    private int clientSocketSndBufSize = NettySystemConfig.socketSndBufSize;
    private int clientSocketRcvBufSize = NettySystemConfig.socketRcvBufSize;

    private int clientChannelMaxIdleTimeSeconds = 120;

    private boolean useTLS;

    private boolean clientCloseSocketIfTimeout = false;
}
