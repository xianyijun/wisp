package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.common.protocol.heartbeat.ConsumeType;
import cn.xianyijun.wisp.common.protocol.heartbeat.MessageModel;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConsumerConnection extends RemotingSerializable {
    private HashSet<Connection> connectionSet = new HashSet<Connection>();
    private ConcurrentMap<String/* Topic */, SubscriptionData> subscriptionTable =
            new ConcurrentHashMap<>();
    private ConsumeType consumeType;
    private MessageModel messageModel;
    private ConsumeWhereEnum consumeFromWhere;

}
