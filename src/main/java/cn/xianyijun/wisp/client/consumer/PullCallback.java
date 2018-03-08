package cn.xianyijun.wisp.client.consumer;

/**
 * The interface Pull callback.
 */
public interface PullCallback {
    /**
     * On success.
     *
     * @param pullResult the pull result
     */
    void onSuccess(final PullResult pullResult);

    /**
     * On exception.
     *
     * @param e the e
     */
    void onException(final Throwable e);
}
