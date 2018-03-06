package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class GetRouteInfoRequestHeader implements CommandCustomHeader {
    private String topic;
}
