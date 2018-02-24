package cn.xianyijun.wisp.store;

/**
 * The interface Message filter.
 * @author xianyijun
 */
public interface MessageFilter {
    /**
     * Is matched by consume queue boolean.
     *
     * @param tagsCode  the tags code
     * @param cqExtUnit the cq ext unit
     * @return the boolean
     */
    boolean isMatchedByConsumeQueue(final Long tagsCode,
                                    final ConsumeQueueExt.CqExtUnit cqExtUnit);
}
