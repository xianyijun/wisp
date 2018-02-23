package cn.xianyijun.wisp.client.hook;

/**
 * The interface Send message hook.
 */
public interface SendMessageHook {
    /**
     * Hook name string.
     *
     * @return the string
     */
    String hookName();

    /**
     * Send message before.
     *
     * @param context the context
     */
    void sendMessageBefore(final SendMessageContext context);

    /**
     * Send message after.
     *
     * @param context the context
     */
    void sendMessageAfter(final SendMessageContext context);
}
