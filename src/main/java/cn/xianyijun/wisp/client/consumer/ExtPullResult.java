package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.ExtMessage;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author xianyijun
 */
@Getter
public class ExtPullResult extends PullResult {
    private final long suggestWhichBrokerId;
    @Setter
    private byte[] messageBinary;

    public ExtPullResult(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset,
                         List<ExtMessage> msgFoundList, final long suggestWhichBrokerId, final byte[] messageBinary) {
        super(pullStatus, nextBeginOffset, minOffset, maxOffset, msgFoundList);
        this.suggestWhichBrokerId = suggestWhichBrokerId;
        this.messageBinary = messageBinary;
    }
}
