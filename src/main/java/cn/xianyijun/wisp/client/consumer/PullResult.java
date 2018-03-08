package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.ExtMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author xianyijun
 */
@AllArgsConstructor
@Getter
public class PullResult {

    private final PullStatus pullStatus;
    private final long nextBeginOffset;
    private final long minOffset;
    private final long maxOffset;
    @Setter
    private List<ExtMessage> msgFoundList;
}
