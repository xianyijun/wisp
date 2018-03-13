package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.common.Pair;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

/**
 * The interface Remoting server.
 * @author xianyijun
 */
public interface RemotingServer extends RemotingService {
    /**
     * Register processor.
     *
     * @param requestCode the request code
     * @param processor   the processor
     * @param executor    the executor
     */
    void registerProcessor(final int requestCode, final NettyRequestProcessor processor,
                           final ExecutorService executor);

    /**
     * Register default processor.
     *
     * @param processor the processor
     * @param executor  the executor
     */
    void registerDefaultProcessor(final NettyRequestProcessor processor, final ExecutorService executor);

    /**
     * Local listen port int.
     *
     * @return the int
     */
    int localListenPort();

    /**
     * Gets processor pair.
     *
     * @param requestCode the request code
     * @return the processor pair
     */
    Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode);

    /**
     * Invoke sync remoting command.
     *
     * @param channel       the channel
     * @param request       the request
     * @param timeoutMillis the timeout millis
     * @return the remoting command
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     */
    RemotingCommand invokeSync(final Channel channel, final RemotingCommand request,
                               final long timeoutMillis) throws InterruptedException, RemotingSendRequestException,
            RemotingTimeoutException;

    /**
     * Invoke async.
     *
     * @param channel        the channel
     * @param request        the request
     * @param timeoutMillis  the timeout millis
     * @param invokeCallback the invoke callback
     * @throws InterruptedException            the interrupted exception
     * @throws RemotingTooMuchRequestException the remoting too much request exception
     * @throws RemotingTimeoutException        the remoting timeout exception
     * @throws RemotingSendRequestException    the remoting send request exception
     */
    void invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis,
                     final InvokeCallback invokeCallback) throws InterruptedException,
            RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * Invoke oneway.
     *
     * @param channel       the channel
     * @param request       the request
     * @param timeoutMillis the timeout millis
     * @throws InterruptedException            the interrupted exception
     * @throws RemotingTooMuchRequestException the remoting too much request exception
     * @throws RemotingTimeoutException        the remoting timeout exception
     * @throws RemotingSendRequestException    the remoting send request exception
     */
    void invokeOneWay(final Channel channel, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
            RemotingSendRequestException;
}
