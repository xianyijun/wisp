package cn.xianyijun.wisp.filter;

import java.util.Map;

/**
 * The interface Evaluation context.
 */
public interface EvaluationContext {


    /**
     * Get object.
     *
     * @param name the name
     * @return the object
     */
    Object get(String name);

    /**
     * Key values map.
     *
     * @return the map
     */
    Map<String, Object> keyValues();
}
