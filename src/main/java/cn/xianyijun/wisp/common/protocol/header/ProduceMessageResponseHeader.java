package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;
import lombok.NonNull;

@Data
public class ProduceMessageResponseHeader implements CommandCustomHeader{
    @NonNull
    private String msgId;
    @NonNull
    private Integer queueId;
    @NonNull
    private Long queueOffset;
    private String transactionId;
}
