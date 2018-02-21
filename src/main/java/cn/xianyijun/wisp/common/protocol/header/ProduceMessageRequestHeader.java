package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;
import lombok.NonNull;

/**
 * @author xianyijun
 */
@Data
public class ProduceMessageRequestHeader implements CommandCustomHeader {

    @NonNull
    private String producerGroup;
    @NonNull
    private String topic;
    @NonNull
    private String defaultTopic;
    @NonNull
    private Integer defaultTopicQueueNums;
    @NonNull
    private Integer queueId;
    @NonNull
    private Integer sysFlag;
    @NonNull
    private Long bornTimestamp;
    @NonNull
    private Integer flag;

    private String properties;
    private Integer reConsumeTimes;
    private boolean unitMode = false;
    private boolean batch = false;
    private Integer maxReConsumeTimes;

}
