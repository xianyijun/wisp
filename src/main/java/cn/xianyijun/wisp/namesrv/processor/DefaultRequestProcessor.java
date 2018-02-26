package cn.xianyijun.wisp.namesrv.processor;

import cn.xianyijun.wisp.namesrv.NameServerController;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 * todo
 */
@Slf4j
public class DefaultRequestProcessor implements NettyRequestProcessor {
    protected final NameServerController nameServerController;

    public DefaultRequestProcessor(NameServerController nameServerController) {
        this.nameServerController = nameServerController;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
