package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class SearchOffsetRequestHeader implements CommandCustomHeader {
    private String topic;
    private Integer queueId;
    private Long timestamp;
}
