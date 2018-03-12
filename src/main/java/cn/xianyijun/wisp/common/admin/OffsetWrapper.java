package cn.xianyijun.wisp.common.admin;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class OffsetWrapper {
    private long brokerOffset;
    private long consumerOffset;

    private long lastTimestamp;

}
