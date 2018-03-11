package cn.xianyijun.wisp.common.protocol.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The enum Consume type.
 */
@AllArgsConstructor
@Getter
public enum ConsumeType {
    /**
     * Consume actively consume type.
     */
    CONSUME_ACTIVELY("PULL"),

    /**
     * Consume passively consume type.
     */
    CONSUME_PASSIVELY("PUSH");

    private String typeCN;
}
