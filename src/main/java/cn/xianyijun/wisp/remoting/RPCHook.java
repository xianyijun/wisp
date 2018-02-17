package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;

/**
 * The interface Rpc hook.
 */
public interface RPCHook {

    /**
     * Do before request.
     *
     * @param remoteAddr the remote addr
     * @param request    the request
     */
    void doBeforeRequest(final String remoteAddr, final RemotingCommand request);

    /**
     * Do after response.
     *
     * @param remoteAddr the remote addr
     * @param request    the request
     * @param response   the response
     */
    void doAfterResponse(final String remoteAddr, final RemotingCommand request,
                         final RemotingCommand response);
}
