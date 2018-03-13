package cn.xianyijun.wisp.store.status;

/**
 * The enum Get message status.
 */
public enum GetMessageStatus {

    /**
     * Found get message status.
     */
    FOUND,

    /**
     * No matched message get message status.
     */
    NO_MATCHED_MESSAGE,

    /**
     * Message was removing get message status.
     */
    MESSAGE_WAS_REMOVING,

    /**
     * Offset found null get message status.
     */
    OFFSET_FOUND_NULL,

    /**
     * Offset overflow badly get message status.
     */
    OFFSET_OVERFLOW_BADLY,

    /**
     * Offset overflow one get message status.
     */
    OFFSET_OVERFLOW_ONE,

    /**
     * Offset too small get message status.
     */
    OFFSET_TOO_SMALL,

    /**
     * No matched logic queue get message status.
     */
    NO_MATCHED_LOGIC_QUEUE,

    /**
     * No message in queue get message status.
     */
    NO_MESSAGE_IN_QUEUE,
}
