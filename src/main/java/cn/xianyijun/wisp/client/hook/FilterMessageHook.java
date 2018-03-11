package cn.xianyijun.wisp.client.hook;

/**
 * The interface Filters message hook.
 */
public interface FilterMessageHook {
    /**
     * Hook name string.
     *
     * @return the string
     */
    String hookName();

    /**
     * Filters message.
     *
     * @param context the context
     */
    void filterMessage(final FilterMessageContext context);
}
