package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class ViewBrokerStatsDataRequestHeader implements CommandCustomHeader {
    private String statsName;
    private String statsKey;
}
