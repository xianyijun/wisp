package cn.xianyijun.wisp.client.producer;

/**
 * The interface Send callback.
 * @author xianyijun
 */
public interface SendCallback {
    /**
     * On success.
     *
     * @param sendResult the send result
     */
    void onSuccess(final SendResult sendResult);

    /**
     * On exception.
     *
     * @param e the e
     */
    void onException(final Throwable e);
}
