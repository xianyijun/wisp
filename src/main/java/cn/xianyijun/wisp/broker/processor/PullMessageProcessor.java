package cn.xianyijun.wisp.broker.processor;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.mqtrace.ConsumeMessageHook;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class PullMessageProcessor implements NettyRequestProcessor {

    private final BrokerController brokerController;
    @Setter
    private List<ConsumeMessageHook> consumeMessageHookList;

    public PullMessageProcessor(final BrokerController brokerController) {
        this.brokerController = brokerController;
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
