package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class WipeWritePermOfBrokerResponseHeader implements CommandCustomHeader {
    private Integer wipeTopicCount;
}
