package cn.xianyijun.wisp.client.admin;

import cn.xianyijun.wisp.client.producer.TopicPublishInfo;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.exception.ClientException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class DefaultAdmin {

    private final ClientInstance clientFactory;
    private long timeoutMillis = 6000;


    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws ClientException {
        try {
            TopicRouteData topicRouteData = this.clientFactory.getClient().getTopicRouteInfoFromNameServer(topic, timeoutMillis);
            if (topicRouteData != null) {
                TopicPublishInfo topicPublishInfo = ClientInstance.topicRouteData2TopicPublishInfo(topic, topicRouteData);
                if (topicPublishInfo != null && topicPublishInfo.ok()) {
                    return topicPublishInfo.getMessageQueueList();
                }
            }
        } catch (Exception e) {
            throw new ClientException("Can not find Message Queue for this topic, " + topic, e);
        }

        throw new ClientException("Unknow why, Can not find Message Queue for this topic, " + topic, null);
    }

}
