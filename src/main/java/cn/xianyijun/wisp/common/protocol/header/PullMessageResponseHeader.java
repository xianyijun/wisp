package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class PullMessageResponseHeader implements CommandCustomHeader {
    private Long suggestWhichBrokerId;
    private Long nextBeginOffset;
    private Long minOffset;
    private Long maxOffset;
}
