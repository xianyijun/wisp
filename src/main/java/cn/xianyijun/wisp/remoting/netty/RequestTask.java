package cn.xianyijun.wisp.remoting.netty;

import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author xianyijun
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestTask implements Runnable {
    private final Runnable runnable;
    private final long createTimestamp = System.currentTimeMillis();
    private final Channel channel;
    private final RemotingCommand request;
    @Setter
    private boolean stopRun = false;


    @Override
    public void run() {
        if (!this.stopRun){
            this.runnable.run();
        }
    }


    public void returnResponse(int code, String remark) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(code, remark);
        response.setOpaque(request.getOpaque());
        this.channel.writeAndFlush(response);
    }
}
