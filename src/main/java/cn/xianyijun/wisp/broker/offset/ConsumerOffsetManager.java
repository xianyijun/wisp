package cn.xianyijun.wisp.broker.offset;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.BrokerPathConfigHelper;
import cn.xianyijun.wisp.common.AbstractConfigManager;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Slf4j
@NoArgsConstructor
@Getter
public class ConsumerOffsetManager extends AbstractConfigManager {
    private static final String TOPIC_GROUP_SEPARATOR = "@";

    private transient BrokerController brokerController;

    private ConcurrentMap<String, ConcurrentMap<Integer, Long>> offsetTable =
            new ConcurrentHashMap<>(512);

    public ConsumerOffsetManager(BrokerController brokerController) {
        this.brokerController = brokerController;
    }

    @Override
    public String configFilePath() {
        return BrokerPathConfigHelper.getConsumerOffsetPath(this.brokerController.getMessageStoreConfig().getStorePathRootDir());
    }

    @Override
    public void decode(String jsonString) {
        if (!StringUtils.isEmpty(jsonString)){
            ConsumerOffsetManager obj = RemotingSerializable.fromJson(jsonString, ConsumerOffsetManager.class);
            if (obj != null){
                this.offsetTable = obj.offsetTable;
            }
        }
    }

    @Override
    public String encode() {
        return this.encode(false);
    }

    @Override
    public String encode(boolean prettyFormat) {
        return RemotingSerializable.toJson(this, prettyFormat);
    }
}
