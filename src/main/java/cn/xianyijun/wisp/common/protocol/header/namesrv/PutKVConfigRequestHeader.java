package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class PutKVConfigRequestHeader implements CommandCustomHeader {
    private String namespace;
    private String key;
    private String value;
}
