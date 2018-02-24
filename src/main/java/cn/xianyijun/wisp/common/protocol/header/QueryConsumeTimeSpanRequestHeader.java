package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class QueryConsumeTimeSpanRequestHeader implements CommandCustomHeader {
    private String topic;
    private String group;
}
