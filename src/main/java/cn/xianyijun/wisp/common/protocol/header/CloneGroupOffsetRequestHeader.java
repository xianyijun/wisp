package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class CloneGroupOffsetRequestHeader implements CommandCustomHeader {
    private String srcGroup;
    private String destGroup;
    private String topic;
    private boolean offline;
}
