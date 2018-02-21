package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class RegisterBrokerResponseHeader implements CommandCustomHeader {
    private String haServerAddr;
    private String masterAddr;
}
