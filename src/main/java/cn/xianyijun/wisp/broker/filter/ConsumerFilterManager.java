package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.BrokerPathConfigHelper;
import cn.xianyijun.wisp.common.AbstractConfigManager;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import cn.xianyijun.wisp.filter.ExpressionType;
import cn.xianyijun.wisp.filter.FilterFactory;
import cn.xianyijun.wisp.filter.utils.BloomFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class ConsumerFilterManager extends AbstractConfigManager {
    private static final long MS_24_HOUR = 24 * 3600 * 1000;

    private transient BrokerController brokerController;

    private transient BloomFilter bloomFilter;

    private ConcurrentMap<String, FilterDataMapByTopic> filterDataByTopic = new ConcurrentHashMap<>(256);

    public ConsumerFilterManager(BrokerController brokerController) {
        this.brokerController = brokerController;
        this.bloomFilter = BloomFilter.createByFn(
                brokerController.getBrokerConfig().getMaxErrorRateOfBloomFilter(),
                brokerController.getBrokerConfig().getExpectConsumerNumUseFilter()
        );
        brokerController.getMessageStoreConfig().setBitMapLengthConsumeQueueExt(
                this.bloomFilter.getM()
        );
    }

    @Override
    public String configFilePath() {
        if (this.brokerController != null) {
            return BrokerPathConfigHelper.getConsumerFilterPath(
                    this.brokerController.getMessageStoreConfig().getStorePathRootDir()
            );
        }
        return BrokerPathConfigHelper.getConsumerFilterPath("./unit_test");

    }

    @Override
    public void decode(String jsonString) {

    }

    @Override
    public String encode() {
        return encode(false);
    }

    @Override
    public String encode(boolean prettyFormat) {
        {
            clean();
        }
        return RemotingSerializable.toJson(this, prettyFormat);
    }

    private void clean() {
        Iterator<Map.Entry<String, FilterDataMapByTopic>> topicIterator = this.filterDataByTopic.entrySet().iterator();
        while (topicIterator.hasNext()) {
            Map.Entry<String, FilterDataMapByTopic> filterDataMapByTopic = topicIterator.next();

            Iterator<Map.Entry<String, ConsumerFilterData>> filterDataIterator
                    = filterDataMapByTopic.getValue().getGroupFilterData().entrySet().iterator();

            while (filterDataIterator.hasNext()) {
                Map.Entry<String, ConsumerFilterData> filterDataByGroup = filterDataIterator.next();

                ConsumerFilterData filterData = filterDataByGroup.getValue();
                if (filterData.howLongAfterDeath() >= (this.brokerController == null ? MS_24_HOUR : this.brokerController.getBrokerConfig().getFilterDataCleanTimeSpan())) {
                    log.info("Remove filter consumer {}, died too long!", filterDataByGroup.getValue());
                    filterDataIterator.remove();
                }
            }

            if (filterDataMapByTopic.getValue().getGroupFilterData().isEmpty()) {
                log.info("Topic has no consumer, remove it! {}", filterDataMapByTopic.getKey());
                topicIterator.remove();
            }
        }
    }

    public final Collection<ConsumerFilterData> get(final String topic) {
        if (!this.filterDataByTopic.containsKey(topic)) {
            return null;
        }
        if (this.filterDataByTopic.get(topic).getGroupFilterData().isEmpty()) {
            return null;
        }

        return this.filterDataByTopic.get(topic).getGroupFilterData().values();
    }

    public ConsumerFilterData get(final String topic, final String consumerGroup) {
        if (!this.filterDataByTopic.containsKey(topic)) {
            return null;
        }
        if (this.filterDataByTopic.get(topic).getGroupFilterData().isEmpty()) {
            return null;
        }

        return this.filterDataByTopic.get(topic).getGroupFilterData().get(consumerGroup);
    }


    @Getter
    @NoArgsConstructor
    @RequiredArgsConstructor
    public static class FilterDataMapByTopic {

        private ConcurrentMap<String, ConsumerFilterData> groupFilterData = new ConcurrentHashMap<>();

        @NonNull
        private String topic;

        protected void reAlive(ConsumerFilterData filterData) {
            long oldDeadTime = filterData.getDeadTime();
            filterData.setDeadTime(0);
            log.info("Re alive consumer filter: {}, oldDeadTime: {}", filterData, oldDeadTime);
        }

        public final ConsumerFilterData get(String consumerGroup) {
            return this.groupFilterData.get(consumerGroup);
        }

    }


    public static ConsumerFilterData build(final String topic, final String consumerGroup,
                                           final String expression, final String type,
                                           final long clientVersion) {
        if (ExpressionType.isTagType(type)) {
            return null;
        }

        ConsumerFilterData consumerFilterData = new ConsumerFilterData();
        consumerFilterData.setTopic(topic);
        consumerFilterData.setConsumerGroup(consumerGroup);
        consumerFilterData.setBornTime(System.currentTimeMillis());
        consumerFilterData.setDeadTime(0);
        consumerFilterData.setExpression(expression);
        consumerFilterData.setExpressionType(type);
        consumerFilterData.setClientVersion(clientVersion);
        try {
            consumerFilterData.setCompiledExpression(
                    FilterFactory.INSTANCE.get(type).compile(expression)
            );
        } catch (Throwable e) {
            log.error("parse error: expr={}, topic={}, group={}, error={}", expression, topic, consumerGroup, e.getMessage());
            return null;
        }

        return consumerFilterData;
    }

}
