package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.remoting.protocol.LanguageCode;
import lombok.Data;

@Data
public class Connection {
    private String clientId;
    private String clientAddr;
    private LanguageCode language;
    private int version;
}
