package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class ProduceMessageRequestHeader implements CommandCustomHeader {

    private String producerGroup;
    private String topic;
    private String defaultTopic;
    private Integer defaultTopicQueueNums;
    private Integer queueId;
    private Integer sysFlag;
    private Long bornTimestamp;
    private Integer flag;

    private String properties;
    private Integer reConsumeTimes;
    private boolean unitMode = false;
    private boolean batch = false;
    private Integer maxReConsumeTimes;

}
