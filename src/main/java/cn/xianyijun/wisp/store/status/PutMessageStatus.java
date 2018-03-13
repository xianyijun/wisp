package cn.xianyijun.wisp.store.status;

/**
 * The enum Put message status.
 */
public enum PutMessageStatus {
    /**
     * Put ok put message status.
     */
    PUT_OK,
    /**
     * Flush disk timeout put message status.
     */
    FLUSH_DISK_TIMEOUT,
    /**
     * Flush slave timeout put message status.
     */
    FLUSH_SLAVE_TIMEOUT,
    /**
     * Slave not available put message status.
     */
    SLAVE_NOT_AVAILABLE,
    /**
     * Service not available put message status.
     */
    SERVICE_NOT_AVAILABLE,
    /**
     * Create mapped file failed put message status.
     */
    CREATE_MAPPED_FILE_FAILED,
    /**
     * Message illegal put message status.
     */
    MESSAGE_ILLEGAL,
    /**
     * Properties size exceeded put message status.
     */
    PROPERTIES_SIZE_EXCEEDED,
    /**
     * Os pagecache busy put message status.
     */
    OS_PAGECACHE_BUSY,
    /**
     * Unknown error put message status.
     */
    UNKNOWN_ERROR,
}
