package cn.xianyijun.wisp.broker.offset;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.BrokerPathConfigHelper;
import cn.xianyijun.wisp.common.AbstractConfigManager;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        if (StringUtils.isEmpty(jsonString)) {
            return;
        }
        ConsumerOffsetManager obj = RemotingSerializable.fromJson(jsonString, ConsumerOffsetManager.class);
        if (obj != null) {
            this.offsetTable = obj.offsetTable;
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

    public Set<String> whichTopicByConsumer(final String group) {
        Set<String> topics = new HashSet<String>();

        for (Map.Entry<String, ConcurrentMap<Integer, Long>> next : this.offsetTable.entrySet()) {
            String topicAtGroup = next.getKey();
            String[] arrays = topicAtGroup.split(TOPIC_GROUP_SEPARATOR);
            if (arrays.length == 2) {
                if (group.equals(arrays[1])) {
                    topics.add(arrays[0]);
                }
            }
        }

        return topics;
    }

    public Map<Integer, Long> queryOffset(final String group, final String topic) {
        String key = topic + TOPIC_GROUP_SEPARATOR + group;
        return this.offsetTable.get(key);
    }


    public long queryOffset(final String group, final String topic, final int queueId) {
        String key = topic + TOPIC_GROUP_SEPARATOR + group;
        ConcurrentMap<Integer, Long> map = this.offsetTable.get(key);
        if (null != map) {
            Long offset = map.get(queueId);
            if (offset != null) {
                return offset;
            }
        }
        return -1;
    }


    public Set<String> whichGroupByTopic(final String topic) {
        Set<String> groups = new HashSet<>();

        for (Map.Entry<String, ConcurrentMap<Integer, Long>> next : this.offsetTable.entrySet()) {
            String topicAtGroup = next.getKey();
            String[] arrays = topicAtGroup.split(TOPIC_GROUP_SEPARATOR);
            if (arrays.length == 2) {
                if (topic.equals(arrays[0])) {
                    groups.add(arrays[1]);
                }
            }
        }
        return groups;
    }


    public Map<Integer, Long> queryMinOffsetInAllGroup(final String topic, final String filterGroups) {

        Map<Integer, Long> queueMinOffset = new HashMap<>();
        Set<String> topicGroups = this.offsetTable.keySet();
        if (!StringUtils.isEmpty(filterGroups)) {
            for (String group : filterGroups.split(",")) {
                topicGroups.removeIf(s -> group.equals(s.split(TOPIC_GROUP_SEPARATOR)[1]));
            }
        }

        for (Map.Entry<String, ConcurrentMap<Integer, Long>> offSetEntry : this.offsetTable.entrySet()) {
            String topicGroup = offSetEntry.getKey();
            String[] topicGroupArr = topicGroup.split(TOPIC_GROUP_SEPARATOR);
            if (topic.equals(topicGroupArr[0])) {
                for (Map.Entry<Integer, Long> entry : offSetEntry.getValue().entrySet()) {
                    long minOffset = this.brokerController.getMessageStore().getMinOffsetInQueue(topic, entry.getKey());
                    if (entry.getValue() >= minOffset) {
                        Long offset = queueMinOffset.get(entry.getKey());
                        if (offset == null) {
                            queueMinOffset.put(entry.getKey(), Math.min(Long.MAX_VALUE, entry.getValue()));
                        } else {
                            queueMinOffset.put(entry.getKey(), Math.min(entry.getValue(), offset));
                        }
                    }
                }
            }

        }
        return queueMinOffset;
    }


    public void cloneOffset(final String srcGroup, final String destGroup, final String topic) {
        ConcurrentMap<Integer, Long> offsets = this.offsetTable.get(topic + TOPIC_GROUP_SEPARATOR + srcGroup);
        if (offsets != null) {
            this.offsetTable.put(topic + TOPIC_GROUP_SEPARATOR + destGroup, new ConcurrentHashMap<>(offsets));
        }
    }


    public void commitOffset(final String clientHost, final String group, final String topic, final int queueId,
                             final long offset) {
        String key = topic + TOPIC_GROUP_SEPARATOR + group;
        this.commitOffset(clientHost, key, queueId, offset);
    }

    private void commitOffset(final String clientHost, final String key, final int queueId, final long offset) {
        ConcurrentMap<Integer, Long> map = this.offsetTable.get(key);
        if (null == map) {
            map = new ConcurrentHashMap<>(32);
            map.put(queueId, offset);
            this.offsetTable.put(key, map);
        } else {
            Long storeOffset = map.put(queueId, offset);
            if (storeOffset != null && offset < storeOffset) {
                log.warn("[commitOffset] update consumer offset less than store. clientHost={}, key={}, queueId={}, requestOffset={}, storeOffset={}", clientHost, key, queueId, offset, storeOffset);
            }
        }
    }
}
