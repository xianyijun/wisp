package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.store.request.DispatchRequest;

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
