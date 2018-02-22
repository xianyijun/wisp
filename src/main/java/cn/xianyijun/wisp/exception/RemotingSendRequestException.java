package cn.xianyijun.wisp.exception;

public class RemotingSendRequestException extends RemotingException {

    public RemotingSendRequestException(String addr) {
        this(addr, null);
    }

    public RemotingSendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
}
