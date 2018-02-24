package cn.xianyijun.wisp.common.protocol.header.filtersrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class RegisterFilterServerResponseHeader implements CommandCustomHeader {
    private String brokerName;
    private long brokerId;
}
