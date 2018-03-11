package cn.xianyijun.wisp.filter;

import cn.xianyijun.wisp.exception.FilterException;

/**
 * The interface Filter.
 *
 * @author xianyijun
 */
public interface Filter {

    /**
     * Compile expression.
     *
     * @param expr the expr
     * @return the expression
     * @throws FilterException the filter exception
     */
    Expression compile(final String expr) throws FilterException;

    /**
     * Of type string.
     *
     * @return the string
     */
    String ofType();
}
