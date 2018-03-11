package cn.xianyijun.wisp.broker.client.listener;

import cn.xianyijun.wisp.broker.client.ConsumerGroupEvent;

/**
 * The interface Consumer ids change listener.
 *
 * @author xianyijun
 */
public interface ConsumerIdsChangeListener {

    /**
     * Handle.
     *
     * @param event the event
     * @param group the group
     * @param args  the args
     */
    void handle(ConsumerGroupEvent event, String group, Object... args);

}
