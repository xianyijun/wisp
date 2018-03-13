package cn.xianyijun.wisp.store;

/**
 * The interface Commit log dispatcher.
 *
 * @author xianyijun
 */
public interface CommitLogDispatcher {

    /**
     * Dispatch.
     *
     * @param request the request
     */
    void dispatch(final DispatchRequest request);
}
