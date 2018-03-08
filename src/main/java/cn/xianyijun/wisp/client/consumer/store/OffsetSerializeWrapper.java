package cn.xianyijun.wisp.client.consumer.store;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OffsetSerializeWrapper extends RemotingSerializable{
    private ConcurrentMap<MessageQueue, AtomicLong> offsetTable =
            new ConcurrentHashMap<>();

}
