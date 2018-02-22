package cn.xianyijun.wisp.common.message;

import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.common.sysflag.MessageSysFlag;
import lombok.Data;

import java.net.SocketAddress;

/**
 * @author xianyijun
 */
@Data
public class ExtMessage extends Message {

    private int queueId;

    private int sysFlag;

    private int storeSize;

    private long queueOffset;
    private long bornTimestamp;
    private SocketAddress bornHost;

    private long storeTimestamp;
    private SocketAddress storeHost;
    private String msgId;
    private long commitLogOffset;
    private int bodyCRC;
    private int reConsumeTimes;

    private long preparedTransactionOffset;


    public static TopicFilterType parseTopicFilterType(final int sysFlag) {
        if ((sysFlag & MessageSysFlag.MULTI_TAGS_FLAG) == MessageSysFlag.MULTI_TAGS_FLAG) {
            return TopicFilterType.MULTI_TAG;
        }
        return TopicFilterType.SINGLE_TAG;
    }
}
