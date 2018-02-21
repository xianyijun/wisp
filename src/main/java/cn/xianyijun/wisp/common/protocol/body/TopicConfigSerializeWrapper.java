package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.DataVersion;
import cn.xianyijun.wisp.common.TopicConfig;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Data
public class TopicConfigSerializeWrapper extends RemotingSerializable {

    private ConcurrentMap<String, TopicConfig> topicConfigTable =
            new ConcurrentHashMap<>();

    private DataVersion dataVersion = new DataVersion();


}
