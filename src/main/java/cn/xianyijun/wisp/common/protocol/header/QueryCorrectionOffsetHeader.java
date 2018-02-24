package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class QueryCorrectionOffsetHeader implements CommandCustomHeader {
    private String filterGroups;
    private String compareGroup;
    private String topic;
}
