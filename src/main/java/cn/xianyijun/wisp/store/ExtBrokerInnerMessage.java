package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.TopicFilterType;
import cn.xianyijun.wisp.common.message.ExtMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExtBrokerInnerMessage extends ExtMessage {
    private String propertiesString;
    private long tagsCode;

    public static long tagsString2tagsCode(final TopicFilterType filter, final String tags) {
        if (StringUtils.isEmpty(tags)) {
            return 0;
        }
        return tags.hashCode();
    }
}
