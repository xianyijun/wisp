package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class ProduceMessageResponseHeader implements CommandCustomHeader {
    private String msgId;
    private Integer queueId;
    private Long queueOffset;
    private String transactionId;
}
