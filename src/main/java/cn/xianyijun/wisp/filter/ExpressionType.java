package cn.xianyijun.wisp.filter;

/**
 * The type Expression type.
 * @author xianyijun
 */
public class  ExpressionType {
    /**
     * The constant SQL92.
     */
    public static final String SQL92 = "SQL92";

    /**
     * The constant TAG.
     */
    public static final String TAG = "TAG";

    /**
     * Is tag type boolean.
     *
     * @param type the type
     * @return the boolean
     */
    public static boolean isTagType(String type) {
        return type == null || TAG.equals(type);
    }
}
