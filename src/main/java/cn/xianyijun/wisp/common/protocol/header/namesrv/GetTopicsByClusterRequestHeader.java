package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode
public class GetTopicsByClusterRequestHeader implements CommandCustomHeader {
    private String cluster;
}
