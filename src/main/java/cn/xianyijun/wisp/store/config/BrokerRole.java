package cn.xianyijun.wisp.store.config;

/**
 * The enum Broker role.
 */
public enum BrokerRole {
    /**
     * Async master broker role.
     */
    ASYNC_MASTER,
    /**
     * Sync master broker role.
     */
    SYNC_MASTER,
    /**
     * Slave broker role.
     */
    SLAVE;
}