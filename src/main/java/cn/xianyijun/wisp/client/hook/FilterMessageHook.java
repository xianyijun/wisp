package cn.xianyijun.wisp.client.hook;

/**
 * The interface Filter message hook.
 */
public interface FilterMessageHook {
    /**
     * Hook name string.
     *
     * @return the string
     */
    String hookName();

    /**
     * Filter message.
     *
     * @param context the context
     */
    void filterMessage(final FilterMessageContext context);
}
