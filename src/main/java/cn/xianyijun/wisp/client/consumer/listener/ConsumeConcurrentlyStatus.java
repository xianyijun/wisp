package cn.xianyijun.wisp.client.consumer.listener;

/**
 * @author xianyijun
 */

public enum ConsumeConcurrentlyStatus {
    /**
     * Success consumption
     */
    CONSUME_SUCCESS,
    /**
     * Failure consumption,later try to consume
     */
    RECONSUME_LATER;
}
