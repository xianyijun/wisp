package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class UnregisterClientRequestHeader implements CommandCustomHeader {

    private String clientID;
    private String producerGroup;
    private String consumerGroup;
}
