package cn.xianyijun.wisp.remoting;

import cn.xianyijun.wisp.exception.RemotingCommandException;

/**
 * The interface Command custom header.
 */
public interface CommandCustomHeader {
    /**
     * Check fields.
     *
     * @throws RemotingCommandException the remoting command exception
     */
    void checkFields() throws RemotingCommandException;
}
