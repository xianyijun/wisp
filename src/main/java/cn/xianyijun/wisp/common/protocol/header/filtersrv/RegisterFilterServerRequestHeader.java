package cn.xianyijun.wisp.common.protocol.header.filtersrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class RegisterFilterServerRequestHeader implements CommandCustomHeader {

    private String filterServerAddr;
}
