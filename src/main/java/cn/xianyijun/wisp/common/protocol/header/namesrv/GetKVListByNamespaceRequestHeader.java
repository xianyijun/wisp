package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class GetKVListByNamespaceRequestHeader implements CommandCustomHeader {
    private String namespace;
}
