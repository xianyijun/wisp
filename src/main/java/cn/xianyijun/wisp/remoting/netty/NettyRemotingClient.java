package cn.xianyijun.wisp.remoting.netty;

import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.ChannelEventListener;
import cn.xianyijun.wisp.remoting.InvokeCallback;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.remoting.RemotingClient;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author xianyijun
 * todo
 */
@Slf4j
@Getter
public class NettyRemotingClient extends AbstractNettyRemoting implements RemotingClient {

    private final NettyClientConfig nettyClientConfig;

    private final ChannelEventListener channelEventListener;

    public NettyRemotingClient(final NettyClientConfig nettyClientConfig) {
        this(nettyClientConfig, null);
    }


    public NettyRemotingClient(final NettyClientConfig nettyClientConfig,
                               final ChannelEventListener channelEventListener) {
        super(nettyClientConfig.getClientOneWaySemaphoreValue(), nettyClientConfig.getClientAsyncSemaphoreValue());
        this.nettyClientConfig = nettyClientConfig;
        this.channelEventListener = channelEventListener;
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerRPCHook(RPCHook rpcHook) {

    }

    @Override
    public ChannelEventListener getChannelEventListener() {
        return channelEventListener;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return null;
    }

    @Override
    RPCHook getRPCHook() {
        return null;
    }

    @Override
    public void updateNameServerAddressList(List<String> addressList) {

    }

    @Override
    public List<String> getNameServerAddressList() {
        return null;
    }

    @Override
    public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        return null;
    }

    @Override
    public void invokeOneWay(String addr, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

    }

    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {

    }

    @Override
    public void invokeAsync(String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {

    }
}
