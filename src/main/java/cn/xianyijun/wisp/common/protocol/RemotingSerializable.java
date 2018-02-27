package cn.xianyijun.wisp.common.protocol;

import com.alibaba.fastjson.JSON;

import java.nio.charset.Charset;

/**
 * The type Remoting serializable.
 * @author xianyijun
 */
public abstract class RemotingSerializable {
    private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    /**
     * Encode byte [ ].
     *
     * @param obj the obj
     * @return the byte [ ]
     */
    public static byte[] encode(final Object obj) {
        final String json = toJson(obj, false);
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    /**
     * To json string.
     *
     * @param obj          the obj
     * @param prettyFormat the pretty format
     * @return the string
     */
    public static String toJson(final Object obj, boolean prettyFormat) {
        return JSON.toJSONString(obj, prettyFormat);
    }

    /**
     * Decode t.
     *
     * @param <T>      the type parameter
     * @param data     the data
     * @param classOfT the class of t
     * @return the t
     */
    public static <T> T decode(final byte[] data, Class<T> classOfT) {
        final String json = new String(data, CHARSET_UTF8);
        return fromJson(json, classOfT);
    }

    /**
     * From json t.
     *
     * @param <T>      the type parameter
     * @param json     the json
     * @param classOfT the class of t
     * @return the t
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return JSON.parseObject(json, classOfT);
    }

    /**
     * Encode byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] encode() {
        final String json = this.toJson();
        if (json != null) {
            return json.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    /**
     * To json string.
     *
     * @return the string
     */
    public String toJson() {
        return toJson(false);
    }

    /**
     * To json string.
     *
     * @param prettyFormat the pretty format
     * @return the string
     */
    public String toJson(final boolean prettyFormat) {
        return toJson(this, prettyFormat);
    }
}