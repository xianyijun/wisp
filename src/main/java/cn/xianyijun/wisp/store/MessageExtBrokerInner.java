package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.utils.StringUtils;
import lombok.Data;

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
