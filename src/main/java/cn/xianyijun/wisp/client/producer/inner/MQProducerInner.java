package cn.xianyijun.wisp.client.producer.inner;

import cn.xianyijun.wisp.client.producer.TopicPublishInfo;

import java.util.Set;

/**
 * The interface Mq producer inner.
 */
public interface MQProducerInner {

    /**
     * Gets publish topic list.
     *
     * @return the publish topic list
     */
    Set<String> getPublishTopicList();

    /**
     * Is publish topic need update boolean.
     *
     * @param topic the topic
     * @return the boolean
     */
    boolean isPublishTopicNeedUpdate(final String topic);

    /**
     * Update topic publish info.
     *
     * @param topic the topic
     * @param info  the info
     */
    void updateTopicPublishInfo(final String topic, final TopicPublishInfo info);


    /**
     * Is unit mode boolean.
     *
     * @return the boolean
     */
    boolean isUnitMode();
}
