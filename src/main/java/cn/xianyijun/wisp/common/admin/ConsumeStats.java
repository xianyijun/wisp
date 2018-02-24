package cn.xianyijun.wisp.common.admin;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConsumeStats extends RemotingSerializable{
    private HashMap<MessageQueue, OffsetWrapper> offsetTable = new HashMap<>();
    private double consumeTps = 0;


    public long computeTotalDiff() {
        long diffTotal = 0L;

        for (Map.Entry<MessageQueue, OffsetWrapper> next : this.offsetTable.entrySet()) {
            long diff = next.getValue().getBrokerOffset() - next.getValue().getConsumerOffset();
            diffTotal += diff;
        }

        return diffTotal;
    }

}
