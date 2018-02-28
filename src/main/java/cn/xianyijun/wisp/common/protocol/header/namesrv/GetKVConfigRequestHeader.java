package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class GetKVConfigRequestHeader implements CommandCustomHeader {
    private String namespace;
    private String key;
}
