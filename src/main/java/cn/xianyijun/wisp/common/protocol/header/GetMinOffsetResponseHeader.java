package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class GetMinOffsetResponseHeader implements CommandCustomHeader {
    private Long offset;
}
