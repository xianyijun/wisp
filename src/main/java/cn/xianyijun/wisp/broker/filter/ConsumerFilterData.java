package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.filter.Expression;
import cn.xianyijun.wisp.filter.utils.BloomFilterData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode
@ToString
public class ConsumerFilterData {

    private String consumerGroup;
    private String topic;
    private String expression;
    private String expressionType;
    private transient Expression compiledExpression;
    private long bornTime;
    private long deadTime = 0;
    private BloomFilterData bloomFilterData;
    private long clientVersion;

    public boolean isDead() {
        return this.deadTime >= this.bornTime;
    }

    public long howLongAfterDeath() {
        if (isDead()) {
            return System.currentTimeMillis() - getDeadTime();
        }
        return -1;
    }

    public boolean isMsgInLive(long msgStoreTime) {
        return msgStoreTime > getBornTime();
    }
}
