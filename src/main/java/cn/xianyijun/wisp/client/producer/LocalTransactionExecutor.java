package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.common.message.Message;

/**
 * The interface Local transaction executor.
 *
 * @author xianyijun
 */
public interface LocalTransactionExecutor {

    /**
     * Execute local transaction branch local transaction state.
     *
     * @param msg the msg
     * @param arg the arg
     * @return the local transaction state
     */
    LocalTransactionState executeLocalTransactionBranch(final Message msg, final Object arg);
}
