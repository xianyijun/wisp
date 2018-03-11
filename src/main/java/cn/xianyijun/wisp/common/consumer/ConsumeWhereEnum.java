package cn.xianyijun.wisp.common.consumer;

/**
 * The enum Consume where enum.
 *
 * @author xianyijun
 */
public enum ConsumeWhereEnum {
    /**
     * Consume from last offset consume where enum.
     */
    CONSUME_FROM_LAST_OFFSET,
    /**
     * Consume from first offset consume where enum.
     */
    CONSUME_FROM_FIRST_OFFSET,
    /**
     * Consume from timestamp consume where enum.
     */
    CONSUME_FROM_TIMESTAMP,
}
