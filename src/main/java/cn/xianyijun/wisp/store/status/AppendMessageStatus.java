package cn.xianyijun.wisp.store.status;

/**
 * The enum Append message status.
 */
public enum AppendMessageStatus {
    /**
     * Put ok append message status.
     */
    PUT_OK,
    /**
     * End of file append message status.
     */
    END_OF_FILE,
    /**
     * Message size exceeded append message status.
     */
    MESSAGE_SIZE_EXCEEDED,
    /**
     * Properties size exceeded append message status.
     */
    PROPERTIES_SIZE_EXCEEDED,
    /**
     * Unknown error append message status.
     */
    UNKNOWN_ERROR,
}
