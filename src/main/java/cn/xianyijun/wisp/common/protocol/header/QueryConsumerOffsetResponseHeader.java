package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class QueryConsumerOffsetResponseHeader implements CommandCustomHeader {
    private Long offset;
}
