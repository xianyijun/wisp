package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class EndTransactionRequestHeader implements CommandCustomHeader {
    private String producerGroup;
    private Long tranStateTableOffset;
    private Long commitLogOffset;
    private Integer commitOrRollback;
    private Boolean fromTransactionCheck = false;
    private String msgId;
    private String transactionId;
}
