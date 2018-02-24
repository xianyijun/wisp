package cn.xianyijun.wisp.filter;

public class   ExpressionType {
    public static final String SQL92 = "SQL92";

    public static final String TAG = "TAG";

    public static boolean isTagType(String type) {
        return type == null || TAG.equals(type);
    }
}
