package cn.xianyijun.wisp.common.message;

import java.util.Map;

public class MessageAccessor {
    public static void clearProperty(final Message msg, final String name) {
        msg.clearProperty(name);
    }

    public static void setProperties(final Message msg, Map<String, String> properties) {
        msg.setProperties(properties);
    }

    public static void putProperty(final Message msg, final String name, final String value) {
        msg.putProperty(name, value);
    }

    public static void setOriginMessageId(final Message msg, String originMessageId) {
        putProperty(msg, MessageConst.PROPERTY_ORIGIN_MESSAGE_ID, originMessageId);
    }

    public static String getOriginMessageId(final Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_ORIGIN_MESSAGE_ID);
    }

    public static void setReConsumeTime(final Message msg, String reconsumeTimes) {
        putProperty(msg, MessageConst.PROPERTY_RECONSUME_TIME, reconsumeTimes);
    }

    public static String getReConsumeTime(final Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_RECONSUME_TIME);
    }

    public static void setMaxReConsumeTimes(final Message msg, String maxReconsumeTimes) {
        putProperty(msg, MessageConst.PROPERTY_MAX_RECONSUME_TIMES, maxReconsumeTimes);
    }

    public static String getMaxReConsumeTimes(final Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_MAX_RECONSUME_TIMES);
    }

    public static void setConsumeStartTimeStamp(final Message msg, String propertyConsumeStartTimeStamp) {
        putProperty(msg, MessageConst.PROPERTY_CONSUME_START_TIMESTAMP, propertyConsumeStartTimeStamp);
    }

    public static String getConsumeStartTimeStamp(final Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_CONSUME_START_TIMESTAMP);
    }

}
