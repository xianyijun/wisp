package cn.xianyijun.wisp.store;

import java.util.Map;

/**
 * The interface Message arriving listener.
 * @author xianyijun
 */
public interface MessageArrivingListener {
    /**
     * Arriving.
     *
     * @param topic        the topic
     * @param queueId      the queue id
     * @param logicOffset  the logic offset
     * @param tagsCode     the tags code
     * @param msgStoreTime the msg store time
     * @param filterBitMap the filter bit map
     * @param properties   the properties
     */
    void arriving(String topic, int queueId, long logicOffset, long tagsCode,
                  long msgStoreTime, byte[] filterBitMap, Map<String, String> properties);

}
