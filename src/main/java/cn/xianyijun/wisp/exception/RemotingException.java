package cn.xianyijun.wisp.exception;

public class RemotingException extends Exception{
    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }

}