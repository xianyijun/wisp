package cn.xianyijun.wisp.filter;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Filters factory.
 */
public class FilterFactory {
    /**
     * The constant INSTANCE.
     */
    public static final FilterFactory INSTANCE = new FilterFactory();

    private static final Map<String, Filter> FILTER_HOLDER = new HashMap<>(4);

    /**
     * Get filter.
     *
     * @param type the type
     * @return the filter
     */
    public Filter get(String type) {
        return FILTER_HOLDER.get(type);
    }
}
