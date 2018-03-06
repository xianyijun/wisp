package cn.xianyijun.wisp.remoting.netty;

import cn.xianyijun.wisp.common.Pair;
import cn.xianyijun.wisp.common.ReleaseOnlyOnceSemaphore;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.ServiceThread;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.ChannelEventListener;
import cn.xianyijun.wisp.remoting.InvokeCallback;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.remoting.protocol.RemotingSysResponseCode;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The type Abstract netty remoting.
 *
 * @author xianyijun
 */
@Slf4j
public abstract class AbstractNettyRemoting {

    final NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();
    /**
     * The Processor table.
     */
    final HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTable =
            new HashMap<>(64);
    /**
     * The Semaphore one way.
     */
    private final Semaphore semaphoreOneWay;
    /**
     * The Semaphore async.
     */
    private final Semaphore semaphoreAsync;
    /**
     * The Response table.
     */
    private final ConcurrentMap<Integer, ResponseFuture> responseTable = new ConcurrentHashMap<>(256);
    /**
     * The Default request processor.
     */
    Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    SslContext sslContext;
    /**
     * Instantiates a new Abstract netty remoting.
     *
     * @param permitsOneWay the permits one way
     * @param permitsAsync  the permits async
     */
    AbstractNettyRemoting(final int permitsOneWay, final int permitsAsync) {
        this.semaphoreOneWay = new Semaphore(permitsOneWay, true);
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
    }

    private void executeInvokeCallback(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
            try {
                executor.submit(() -> {
                    try {
                        responseFuture.executeInvokeCallback();
                    } catch (Throwable e) {
                        log.warn("execute callback in executor exception, and callback throw", e);
                    } finally {
                        responseFuture.release();
                    }
                });
            } catch (Exception e) {
                runInThisThread = true;
                log.warn("execute callback in executor exception, maybe executor busy", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeInvokeCallback();
            } catch (Throwable e) {
                log.warn("executeInvokeCallback Exception", e);
            } finally {
                responseFuture.release();
            }
        }
    }

    void putNettyEvent(final NettyEvent event) {
        this.nettyEventExecutor.putNettyEvent(event);
    }

    /**
     * Do invoke async.
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
    void doInvokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback)
            throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        final int opaque = request.getOpaque();
        boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final ReleaseOnlyOnceSemaphore once = new ReleaseOnlyOnceSemaphore(this.semaphoreAsync);

            final ResponseFuture responseFuture = new ResponseFuture(opaque, timeoutMillis, invokeCallback, once);
            this.responseTable.put(opaque, responseFuture);
            try {
                channel.writeAndFlush(request).addListener((ChannelFutureListener) listener -> {
                    if (listener.isSuccess()) {
                        responseFuture.setSendRequestOK(true);
                        return;
                    } else {
                        responseFuture.setSendRequestOK(false);
                    }
                    log.info("[AbstractNettyRemoting] doInvokeAsync listener putResponse failure");
                    responseFuture.putResponse(null);
                    responseTable.remove(opaque);
                    try {
                        executeInvokeCallback(responseFuture);
                    } catch (Throwable e) {
                        log.warn("execute callback in writeAndFlush addListener, and callback throw", e);
                    } finally {
                        responseFuture.release();
                    }

                    log.warn("send a request command to channel <{}> failed.", RemotingHelper.parseChannelRemoteAddr(channel));
                });
            } catch (Exception e) {
                responseFuture.release();
                log.warn("send a request command to channel <" + RemotingHelper.parseChannelRemoteAddr(channel) + "> Exception", e);
                throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingTooMuchRequestException("invokeAsyncImpl invoke too fast");
            } else {
                String info =
                        String.format("invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                                timeoutMillis,
                                this.semaphoreAsync.getQueueLength(),
                                this.semaphoreAsync.availablePermits()
                        );
                log.warn(info);
                throw new RemotingTimeoutException(info);
            }
        }
    }

    /**
     * Do invoke sync remoting command.
     *
     * @param channel       the channel
     * @param request       the request
     * @param timeoutMillis the timeout millis
     * @return the remoting command
     * @throws InterruptedException         the interrupted exception
     * @throws RemotingSendRequestException the remoting send request exception
     * @throws RemotingTimeoutException     the remoting timeout exception
     */
    RemotingCommand doInvokeSync(final Channel channel, final RemotingCommand request,
                                 final long timeoutMillis)
            throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        final int opaque = request.getOpaque();
        log.info("[AbstractNettyRemoting] class :{} , doInvokeSync , channel :{} , request: {} , timeoutMillis :{} ,isOneWay:{} ,",this.getClass().getSimpleName(), channel, request, timeoutMillis, request.isOneWayRPC());
        try {
            final ResponseFuture responseFuture = new ResponseFuture(opaque, timeoutMillis, null, null);
            log.info("[AbstractNettyRemoting] class:{} , doInvokeSync , opaque: {} , request:{} , response:{} ",this.getClass().getSimpleName(), opaque ,request, responseFuture);
            this.responseTable.put(opaque, responseFuture);
            final SocketAddress addr = channel.remoteAddress();
            log.info("{}.doInvokeSync , type:{} request:{}",this.getClass().getSimpleName(),request.getType(),request);
            channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    responseFuture.setSendRequestOK(true);
                    return;
                } else {
                    responseFuture.setSendRequestOK(false);
                }

                ResponseFuture prev = responseTable.remove(opaque);
                log.info("[AbstractNettyRemoting.donInvokeSync] failure putResponse prev :{}", prev);
                responseFuture.setCause(f.cause());
                responseFuture.putResponse(null);
                log.warn("send a request command to channel <" + addr + "> failed.");
            });

            RemotingCommand responseCommand = responseFuture.waitResponse(timeoutMillis);
            if (null == responseCommand) {
                if (responseFuture.isSendRequestOK()) {
                    throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr), timeoutMillis,
                            responseFuture.getCause());
                } else {
                    throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
                }
            }

            return responseCommand;
        } finally {
            this.responseTable.remove(opaque);
        }
    }

    /**
     * Do invoke one way.
     *
     * @param channel       the channel
     * @param request       the request
     * @param timeoutMillis the timeout millis
     * @throws InterruptedException            the interrupted exception
     * @throws RemotingTooMuchRequestException the remoting too much request exception
     * @throws RemotingTimeoutException        the remoting timeout exception
     * @throws RemotingSendRequestException    the remoting send request exception
     */
    void doInvokeOneWay(Channel channel, RemotingCommand request, long timeoutMillis)
            throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        request.markOneWayRPC();
        boolean acquired = this.semaphoreOneWay.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final ReleaseOnlyOnceSemaphore once = new ReleaseOnlyOnceSemaphore(this.semaphoreOneWay);
            try {
                channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                    once.release();
                    if (!f.isSuccess()) {
                        log.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                    }
                });
            } catch (Exception e) {
                once.release();
                log.warn("write send a request command to channel <" + channel.remoteAddress() + "> failed.");
                throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingTooMuchRequestException("doInvokeOneWay invoke too fast");
            } else {
                String info = String.format(
                        "doInvokeOneWay tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                        timeoutMillis,
                        this.semaphoreOneWay.getQueueLength(),
                        this.semaphoreOneWay.availablePermits()
                );
                log.warn(info);
                throw new RemotingTimeoutException(info);
            }
        }
    }

    /**
     * Process message received.
     *
     * @param ctx the ctx
     * @param msg the msg
     * @throws Exception the exception
     */
    void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) {
        if (msg == null) {
            return;
        }
        log.info("[AbstractNettyRemoting] processMessageReceived, type: {} , msg :{} ", msg.getType(), msg);
        switch (msg.getType()) {
            case REQUEST_COMMAND:
                processRequestCommand(ctx, msg);
                break;
            case RESPONSE_COMMAND:
                processResponseCommand(ctx, msg);
                break;
            default:
                break;
        }
    }

    /**
     * Process request command.
     *
     * @param ctx the ctx
     * @param cmd the cmd
     */
    private void processRequestCommand(final ChannelHandlerContext ctx, final RemotingCommand cmd) {
        log.info("[processRequestCommand] process RequestCommand ,code :{} ,body :{}, remark:{} , header:{} " ,cmd.getCode() , cmd.getBody() == null ? "": new String(cmd.getBody()),cmd.getRemark(), cmd.getCustomHeader());
        final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
        final Pair<NettyRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;

        log.info("[AbstractNettyRemoting.processRequestCommand] pair, code: {} ,processor:{}", cmd.getCode() ,pair.getFirst().getClass().getSimpleName());
        final int opaque = cmd.getOpaque();

        Runnable run = () -> {
            try {
                RPCHook rpcHook = AbstractNettyRemoting.this.getRPCHook();
                if (rpcHook != null) {
                    rpcHook.doBeforeRequest(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd);
                }

                final RemotingCommand response = pair.getFirst().processRequest(ctx, cmd);
                if (rpcHook != null) {
                    rpcHook.doAfterResponse(RemotingHelper.parseChannelRemoteAddr(ctx.channel()), cmd, response);
                }

                if (!cmd.isOneWayRPC()) {
                    if (response != null) {
                        response.setOpaque(opaque);
                        response.markResponseType();
                        try {
                            ctx.writeAndFlush(response);
                        } catch (Throwable e) {
                            log.error("process request over, but response failed", e);
                            log.error(cmd.toString());
                            log.error(response.toString());
                        }
                    }
                }
            } catch (Throwable e) {
                log.error("process request exception", e);
                log.error(cmd.toString());

                if (cmd.isOneWayRPC()) {
                    final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_ERROR,
                            RemotingHelper.exceptionSimpleDesc(e));
                    response.setOpaque(opaque);
                    ctx.writeAndFlush(response);
                }
            }
        };

        if (pair.getFirst().rejectRequest()) {
            final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY,
                    "[REJECTREQUEST]system busy, start flow control for a while");
            response.setOpaque(opaque);
            ctx.writeAndFlush(response);
            return;
        }

        try {
            final RequestTask requestTask = new RequestTask(run, ctx.channel(), cmd);
            pair.getSecond().submit(requestTask);
        } catch (RejectedExecutionException e) {
            if ((System.currentTimeMillis() % 10000) == 0) {
                log.warn(RemotingHelper.parseChannelRemoteAddr(ctx.channel())
                        + ", too many requests and system thread pool busy, RejectedExecutionException "
                        + pair.getSecond().toString()
                        + " request code: " + cmd.getCode());
            }

            if (cmd.isOneWayRPC()) {
                final RemotingCommand response = RemotingCommand.createResponseCommand(RemotingSysResponseCode.SYSTEM_BUSY,
                        "[OVERLOAD]system busy, start flow control for a while");
                response.setOpaque(opaque);
                ctx.writeAndFlush(response);
            }
        }
    }

    /**
     * Process response from remote peer to the previous issued requests.
     *
     * @param ctx channel handler context.
     * @param response response command instance.
     */
    private void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand response) {
        log.info("[AbstractResponseCommand] processResponseCommand , response : {}, responseTable: {}", response, responseTable);
        final int opaque = response.getOpaque();
        final ResponseFuture responseFuture = responseTable.get(opaque);
        if (responseFuture != null) {
            log.info("[AbstractNettyRemoting] class:{} putResponse response:{} ",this.getClass().getSimpleName(),response);
            responseFuture.setResponseCommand(response);

            responseTable.remove(opaque);

            if (responseFuture.getInvokeCallback() != null) {
                executeInvokeCallback(responseFuture);
            } else {
                responseFuture.putResponse(response);
                responseFuture.release();
            }
        } else {
            log.warn("receive response, but not matched any request, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            log.warn(response.toString());
        }
    }

    void scanResponseTable() {
        final List<ResponseFuture> rfList = new LinkedList<>();
        Iterator<Map.Entry<Integer, ResponseFuture>> it = this.responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ResponseFuture> next = it.next();
            ResponseFuture rep = next.getValue();

            if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {
                rep.release();
                it.remove();
                rfList.add(rep);
                log.warn("remove timeout request, " + rep);
            }
        }

        for (ResponseFuture rf : rfList) {
            try {
                executeInvokeCallback(rf);
            } catch (Throwable e) {
                log.warn("scanResponseTable, operationComplete Exception", e);
            }
        }
    }

    /**
     * Gets channel event listener.
     *
     * @return the channel event listener
     */
    public abstract ChannelEventListener getChannelEventListener();

    /**
     * Gets callback executor.
     *
     * @return the callback executor
     */
    public abstract ExecutorService getCallbackExecutor();

    /**
     * Gets rpc hook.
     *
     * @return the rpc hook
     */
    abstract RPCHook getRPCHook();


    class NettyEventExecutor extends ServiceThread {
        private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<>();
        private final int maxSize = 10000;

        public void putNettyEvent(final NettyEvent event) {
            if (this.eventQueue.size() <= maxSize) {
                this.eventQueue.add(event);
            } else {
                log.warn("event queue size[{}] enough, so drop this event {}", this.eventQueue.size(), event.toString());
            }
        }

        @Override
        public void run() {
            log.info(this.getServiceName() + " service started");

            final ChannelEventListener listener = AbstractNettyRemoting.this.getChannelEventListener();

            while (!this.isStopped()) {
                try {
                    NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        switch (event.getType()) {
                            case IDLE:
                                listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CLOSE:
                                listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getRemoteAddr(), event.getChannel());
                                break;
                            default:
                                break;

                        }
                    }
                } catch (Exception e) {
                    log.warn(this.getServiceName() + " service has exception. ", e);
                }
            }

            log.info(this.getServiceName() + " service end");
        }

        @Override
        public String getServiceName() {
            return NettyEventExecutor.class.getSimpleName();
        }
    }
}
