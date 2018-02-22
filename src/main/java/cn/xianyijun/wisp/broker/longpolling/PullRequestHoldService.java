package cn.xianyijun.wisp.broker.longpolling;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.common.ServiceThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class PullRequestHoldService extends ServiceThread {

    private static final String TOPIC_QUEUE_ID_SEPARATOR = "@";
    private final BrokerController brokerController;

    public PullRequestHoldService(final BrokerController brokerController) {
        this.brokerController = brokerController;
    }

    public void notifyMessageArriving(final String topic, final int queueId, final long maxOffset, final Long tagsCode,
                                      long msgStoreTime, byte[] filterBitMap, Map<String, String> properties) {

    }

    @Override
    public String getServiceName() {
        return PullRequestHoldService.class.getSimpleName();
    }

    @Override
    public void run() {

    }
}
