package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class ResetOffsetRequestHeader implements CommandCustomHeader {

    private String topic;
    private String group;
    private long timestamp;
    private boolean isForce;

}
