package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.common.message.ExtMessage;

/**
 * The interface Transaction check listener.
 * @author xianyijun
 */
public interface TransactionCheckListener {
    /**
     * Check local transaction state local transaction state.
     *
     * @param msg the msg
     * @return the local transaction state
     */
    LocalTransactionState checkLocalTransactionState(final ExtMessage msg);
}
