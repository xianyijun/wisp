package cn.xianyijun.wisp.filter;

/**
 * The interface Expression.
 * @author xianyijun
 */
public interface Expression {

    /**
     * Evaluate object.
     *
     * @param context the context
     * @return the object
     * @throws Exception the exception
     */
    Object evaluate(EvaluationContext context) throws Exception;

}
