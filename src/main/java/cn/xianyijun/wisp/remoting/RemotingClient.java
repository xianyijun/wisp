package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;

import java.util.List;

/**
 * The interface Remoting client.
 *
 * @author xianyijun
 */
public interface RemotingClient extends RemotingService{
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

}
