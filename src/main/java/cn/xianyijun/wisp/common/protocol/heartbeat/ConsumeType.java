package cn.xianyijun.wisp.common.protocol.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ConsumeType {
    CONSUME_ACTIVELY("PULL"),

    CONSUME_PASSIVELY("PUSH");

    private String typeCN;
}
