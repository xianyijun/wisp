package cn.xianyijun.wisp.broker.mqtrace;

/**
 * The interface Produce message hook.
 *
 * @author xianyijun
 */
public interface ProduceMessageHook {
    /**
     * Send message before.
     *
     * @param context the context
     */
    public void sendMessageBefore(final ProduceMessageContext context);

    /**
     * Send message after.
     *
     * @param context the context
     */
    public void sendMessageAfter(final ProduceMessageContext context);

}
