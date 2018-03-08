package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class ConsumerSendMsgBackRequestHeader implements CommandCustomHeader {
    private Long offset;
    private String group;
    private Integer delayLevel;
    private String originMsgId;
    private String originTopic;
    private boolean unitMode = false;
    private Integer maxReConsumeTimes;

}
