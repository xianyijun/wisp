package cn.xianyijun.wisp.exception;

public class RemotingConnectException extends RemotingException {
    public RemotingConnectException(String addr) {
        this(addr, null);
    }

    public RemotingConnectException(String addr, Throwable cause) {
        super("connect to <" + addr + "> failed", cause);
    }

}
