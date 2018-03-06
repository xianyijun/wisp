package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class CheckTransactionStateRequestHeader implements CommandCustomHeader {
    private Long tranStateTableOffset;
    private Long commitLogOffset;
    private String msgId;
    private String transactionId;
}
