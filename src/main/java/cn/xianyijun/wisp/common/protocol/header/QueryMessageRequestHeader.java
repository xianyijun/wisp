package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class QueryMessageRequestHeader implements CommandCustomHeader {
    private String topic;
    private String key;
    private Integer maxNum;
    private Long beginTimestamp;
    private Long endTimestamp;

}
