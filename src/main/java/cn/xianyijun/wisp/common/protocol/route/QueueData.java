package cn.xianyijun.wisp.common.protocol.route;

import lombok.Data;

@Data
public class QueueData implements Comparable<QueueData>{
    private String brokerName;
    private int readQueueNums;
    private int writeQueueNums;
    private int perm;
    private int topicSynFlag;

    @Override
    public int compareTo(QueueData o) {
        return this.brokerName.compareTo(o.getBrokerName());
    }
}
