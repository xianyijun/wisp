package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.filter.Expression;
import cn.xianyijun.wisp.filter.utils.BloomFilterData;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collections;

/**
 * @author xianyijun
 */
@Data
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

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, Collections.emptyList());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, Collections.emptyList());
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
