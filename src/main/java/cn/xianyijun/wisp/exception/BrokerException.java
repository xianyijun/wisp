package cn.xianyijun.wisp.exception;

public class BrokerException extends Exception {
    private static final long serialVersionUID = 5975020272601250368L;
    private final int responseCode;
    private final String errorMessage;

    public BrokerException(int responseCode, String errorMessage) {
        super();
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}