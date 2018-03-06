package cn.xianyijun.wisp.common;

import cn.xianyijun.wisp.common.constant.PermName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author xianyijun
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TopicConfig {

    private static final String SEPARATOR = " ";

    public static int defaultReadQueueNums = 16;
    public static int defaultWriteQueueNums = 16;
    private String topicName;
    private int readQueueNums = defaultReadQueueNums;
    private int writeQueueNums = defaultWriteQueueNums;

    private int perm = PermName.PERM_READ | PermName.PERM_WRITE;

    private boolean order = false;

    private TopicFilterType topicFilterType = TopicFilterType.SINGLE_TAG;
    private int topicSysFlag = 0;

    public TopicConfig(String topicName) {
        this.topicName = topicName;
    }

    public TopicConfig(String topicName, int readQueueNums, int writeQueueNums, int perm) {
        this.topicName = topicName;
        this.readQueueNums = readQueueNums;
        this.writeQueueNums = writeQueueNums;
        this.perm = perm;
    }
}
