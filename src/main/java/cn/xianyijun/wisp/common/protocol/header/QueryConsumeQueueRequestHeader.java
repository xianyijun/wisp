package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class QueryConsumeQueueRequestHeader implements CommandCustomHeader {

    private String topic;
    private int queueId;
    private long index;
    private int count;
    private String consumerGroup;
}
