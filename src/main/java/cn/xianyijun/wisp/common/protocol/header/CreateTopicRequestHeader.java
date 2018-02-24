package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class CreateTopicRequestHeader implements CommandCustomHeader {

    private String topic;
    private String defaultTopic;
    private Integer readQueueNums;
    private Integer writeQueueNums;
    private Integer perm;
    private String topicFilterType;
    private Integer topicSysFlag;
    private Boolean order = false;

    public TopicFilterType getTopicFilterTypeEnum() {
        return TopicFilterType.valueOf(this.topicFilterType);
    }
}
