package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.common.message.ExtMessage;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author xianyijun
 */
@Data
public class MessageExtBrokerInner extends ExtMessage {
    private String propertiesString;
    private long tagsCode;

    public static long tagsString2tagsCode(final TopicFilterType filter, final String tags) {
        if (StringUtils.isEmpty(tags)) {
            return 0;
        }
        return tags.hashCode();
    }
}
