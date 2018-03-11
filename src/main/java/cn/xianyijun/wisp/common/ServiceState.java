package cn.xianyijun.wisp.common;

/**
 * The enum Service state.
 */
public enum ServiceState {
    /**
     * Create just service state.
     */
    CREATE_JUST,
    /**
     * Running service state.
     */
    RUNNING,
    /**
     * Shutdown already service state.
     */
    SHUTDOWN_ALREADY,
    /**
     * Start failed service state.
     */
    START_FAILED;
}