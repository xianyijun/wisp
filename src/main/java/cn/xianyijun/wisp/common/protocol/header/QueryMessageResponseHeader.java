package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class QueryMessageResponseHeader implements CommandCustomHeader {
    private Long indexLastUpdateTimestamp;
    private Long indexLastUpdatePhyOffset;
}
