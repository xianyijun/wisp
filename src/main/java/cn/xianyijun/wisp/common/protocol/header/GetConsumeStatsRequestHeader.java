package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class GetConsumeStatsRequestHeader implements CommandCustomHeader {
    private String consumerGroup;
    private String topic;
}
