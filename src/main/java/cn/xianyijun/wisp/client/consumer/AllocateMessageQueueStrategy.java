package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.MessageQueue;

import java.util.List;

/**
 * The interface Allocate message queue strategy.
 *
 * @author xianyijun
 */
public interface AllocateMessageQueueStrategy {

    /**
     * Allocate list.
     *
     * @param consumerGroup the consumer group
     * @param currentCID    the current cid
     * @param mqAll         the mq all
     * @param cidAll        the cid all
     * @return the list
     */
    List<MessageQueue> allocate(
            final String consumerGroup,
            final String currentCID,
            final List<MessageQueue> mqAll,
            final List<String> cidAll
    );

    /**
     * Gets name.
     *
     * @return the name
     */
    String getName();
}
