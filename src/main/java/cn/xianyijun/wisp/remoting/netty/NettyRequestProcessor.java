package cn.xianyijun.wisp.remoting.netty;

import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * The interface Netty request processor.
 */
public interface NettyRequestProcessor {
    /**
     * Process request remoting command.
     *
     * @param ctx     the ctx
     * @param request the request
     * @return the remoting command
     * @throws Exception the exception
     */
    RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
            throws Exception;

    /**
     * Reject request boolean.
     *
     * @return the boolean
     */
    boolean rejectRequest();
}
