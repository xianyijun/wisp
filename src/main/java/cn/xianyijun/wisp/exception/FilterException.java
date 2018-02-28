package cn.xianyijun.wisp.exception;

public class FilterException extends Exception {
    private final int responseCode;
    private final String errorMessage;

    public FilterException(String errorMessage, Throwable cause) {
        super(cause);
        this.responseCode = -1;
        this.errorMessage = errorMessage;
    }
}
