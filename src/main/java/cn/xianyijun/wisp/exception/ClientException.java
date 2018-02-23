package cn.xianyijun.wisp.exception;

public class ClientException extends Exception {
    private static final long serialVersionUID = -5758410930844185841L;
    private int responseCode;
    private String errorMessage;

    public ClientException(String errorMessage, Throwable cause) {
        super(errorMessage);
        this.responseCode = -1;
        this.errorMessage = errorMessage;
    }

    public ClientException(int responseCode, String errorMessage) {
        super(errorMessage);
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public ClientException setResponseCode(final int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
