package cn.xianyijun.wisp.common.admin;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class RollbackStats {

    private String brokerName;
    private long queueId;
    private long brokerOffset;
    private long consumerOffset;
    private long timestampOffset;
    private long rollbackOffset;
}
