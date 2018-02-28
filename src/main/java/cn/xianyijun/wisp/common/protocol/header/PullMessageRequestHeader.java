package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class PullMessageRequestHeader implements CommandCustomHeader {
    private String consumerGroup;
    private String topic;
    private Integer queueId;
    private Long queueOffset;
    private Integer maxMsgNums;
    private Integer sysFlag;
    private Long commitOffset;
    private Long suspendTimeoutMillis;
    private String subscription;
    private Long subVersion;
    private String expressionType;
}
