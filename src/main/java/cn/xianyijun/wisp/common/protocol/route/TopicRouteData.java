package cn.xianyijun.wisp.common.protocol.route;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class TopicRouteData extends RemotingSerializable {
    private String orderTopicConf;
    private List<QueueData> queueDatas;
    private List<BrokerData> brokerDatas;
    private HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;

    public TopicRouteData cloneTopicRouteData() {
        TopicRouteData topicRouteData = new TopicRouteData();
        topicRouteData.setQueueDatas(new ArrayList<>());
        topicRouteData.setBrokerDatas(new ArrayList<>());
        topicRouteData.setFilterServerTable(new HashMap<>());
        topicRouteData.setOrderTopicConf(this.orderTopicConf);

        if (this.queueDatas != null) {
            topicRouteData.getQueueDatas().addAll(this.queueDatas);
        }

        if (this.brokerDatas != null) {
            topicRouteData.getBrokerDatas().addAll(this.brokerDatas);
        }

        if (this.filterServerTable != null) {
            topicRouteData.getFilterServerTable().putAll(this.filterServerTable);
        }

        return topicRouteData;
    }

}

