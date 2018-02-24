package cn.xianyijun.wisp.common.admin;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class TopicOffset {
    private long minOffset;
    private long maxOffset;
    private long lastUpdateTimestamp;
}
