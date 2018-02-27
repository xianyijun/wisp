package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * The interface Remoting client.
 *
 * @author xianyijun
 */
public interface RemotingClient extends RemotingService {
    /**
     * Update name server address list.
     *
     * @param addressList the address list
     */
    void updateNameServerAddressList(final List<String> addressList);

    /**
     * Gets name server address list.
     *
     * @return the name server address list
     */
    List<String> getNameServerAddressList();

    /**
     * Invoke sync remoting command.
     *
     * @param addr          the addr
     * @param request       the request
     * @param timeoutMillis the timeout millis
     * @return the remoting command
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingConnectException     the remoting connect exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     */
    RemotingCommand invokeSync(final String addr, final RemotingCommand request,
                               final long timeoutMillis) throws InterruptedException, RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException;

    /**
     * Invoke one way.
     *
     * @param addr          the addr
     * @param request       the request
     * @param timeoutMillis the timeout millis
     * @throws InterruptedException            the interrupted exception
     * @throws RemotingConnectException        the remoting connect exception
     * @throws RemotingTooMuchRequestException the remoting too much request exception
     * @throws RemotingTimeoutException        the remoting timeout exception
     * @throws RemotingSendRequestException    the remoting send request exception
     */
    void invokeOneWay(final String addr, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
            RemotingTimeoutException, RemotingSendRequestException;


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
     * Invoke async.
     *
     * @param addr           the addr
     * @param request        the request
     * @param timeoutMillis  the timeout millis
     * @param invokeCallback the invoke callback
     * @throws InterruptedException            the interrupted exception
     * @throws RemotingConnectException        the remoting connect exception
     * @throws RemotingTooMuchRequestException the remoting too much request exception
     * @throws RemotingTimeoutException        the remoting timeout exception
     * @throws RemotingSendRequestException    the remoting send request exception
     */
    void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis,
                     final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException,
            RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;


    /**
     * Sets callback executor.
     *
     * @param callbackExecutor the callback executor
     */
    void setCallbackExecutor(final ExecutorService callbackExecutor);
}
