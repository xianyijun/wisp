package cn.xianyijun.wisp.broker.client;

import cn.xianyijun.wisp.broker.BrokerController;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
public class DefaultConsumerIdsChangeListener implements ConsumerIdsChangeListener {
    private final BrokerController brokerController;

    @Override
    public void handle(ConsumerGroupEvent event, String group, Object... args) {
        if (event == null) {
            return;
        }
    }
}
