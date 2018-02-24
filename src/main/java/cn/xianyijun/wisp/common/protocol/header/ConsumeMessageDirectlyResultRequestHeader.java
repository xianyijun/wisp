package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class ConsumeMessageDirectlyResultRequestHeader implements CommandCustomHeader {
    private String consumerGroup;
    private String clientId;
    private String msgId;
    private String brokerName;
}
