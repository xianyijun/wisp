package cn.xianyijun.wisp.common.protocol.heartbeat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xianyijun
 */
@NoArgsConstructor
@Data
public class SubscriptionData implements Comparable<SubscriptionData> {
    public final static String SUB_ALL = "*";
    private boolean classFilterMode = false;
    private String topic;
    private String subString;
    private Set<String> tagsSet = new HashSet<String>();
    private Set<Integer> codeSet = new HashSet<Integer>();
    private long subVersion = System.currentTimeMillis();
    private String expressionType;

    @JSONField(serialize = false)
    private String filterClassSource;

    public SubscriptionData(String topic, String subString) {
        super();
        this.topic = topic;
        this.subString = subString;
    }

    @Override
    public int compareTo(SubscriptionData other) {
        String thisValue = this.topic + "@" + this.subString;
        String otherValue = other.topic + "@" + other.subString;
        return thisValue.compareTo(otherValue);
    }
}