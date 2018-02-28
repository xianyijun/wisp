package cn.xianyijun.wisp.filter;

import java.util.HashMap;
import java.util.Map;

public class FilterFactory {
    public static final FilterFactory INSTANCE = new FilterFactory();

    private static final Map<String, Filter> FILTER_HOLDER = new HashMap<>(4);

    public Filter get(String type) {
        return FILTER_HOLDER.get(type);
    }
}
