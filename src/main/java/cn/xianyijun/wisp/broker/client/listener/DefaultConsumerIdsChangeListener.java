package cn.xianyijun.wisp.broker.client.listener;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.client.ConsumerGroupEvent;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

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
        switch (event) {
            case CHANGE:
                if (args == null || args.length < 1) {
                    return;
                }
                List<Channel> channels = (List<Channel>) args[0];
                if (channels != null && brokerController.getBrokerConfig().isNotifyConsumerIdsChangedEnable()) {
                    for (Channel chl : channels) {
                        this.brokerController.getBrokerClient().notifyConsumerIdsChanged(chl, group);
                    }
                }
                break;
            case UNREGISTER:
                this.brokerController.getConsumerFilterManager().unRegister(group);
                break;
            case REGISTER:
                if (args == null || args.length < 1) {
                    return;
                }
                Collection<SubscriptionData> subscriptionDataList = (Collection<SubscriptionData>) args[0];
                this.brokerController.getConsumerFilterManager().register(group, subscriptionDataList);
                break;
            default:
                throw new RuntimeException("Unknown event " + event);
        }

    }
}
