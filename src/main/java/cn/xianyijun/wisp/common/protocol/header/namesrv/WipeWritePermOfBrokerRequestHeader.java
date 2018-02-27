package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class WipeWritePermOfBrokerRequestHeader implements CommandCustomHeader {
    private String brokerName;
}
