package cn.xianyijun.wisp.broker.longpolling;

import cn.xianyijun.wisp.store.MessageArrivingListener;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
public class NotifyMessageArrivingListener implements MessageArrivingListener {

    private final PullRequestHoldService pullRequestHoldService;

    @Override
    public void arriving(String topic, int queueId, long logicOffset, long tagsCode, long msgStoreTime, byte[] filterBitMap, Map<String, String> properties) {
        this.pullRequestHoldService.notifyMessageArriving(topic, queueId, logicOffset, tagsCode,
                msgStoreTime, filterBitMap, properties);

    }
}
