package cn.xianyijun.wisp.broker.mqtrace;

/**
 * The interface Consume message hook.
 */
public interface ConsumeMessageHook {
    /**
     * Hook name string.
     *
     * @return the string
     */
    String hookName();

    /**
     * Consume message before.
     *
     * @param context the context
     */
    void consumeMessageBefore(final ConsumeMessageContext context);

    /**
     * Consume message after.
     *
     * @param context the context
     */
    void consumeMessageAfter(final ConsumeMessageContext context);
}
