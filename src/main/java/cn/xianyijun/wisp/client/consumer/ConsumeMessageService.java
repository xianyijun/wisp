package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.body.ConsumeMessageDirectlyResult;

import java.util.List;

/**
 * The interface Consume message service.
 *
 * @author xianyijun
 */
public interface ConsumeMessageService {

    /**
     * Start.
     */
    void start();

    /**
     * Shutdown.
     */
    void shutdown();

    /**
     * Inc core pool size.
     */
    void incCorePoolSize();

    /**
     * Dec core pool size.
     */
    void decCorePoolSize();

    /**
     * Gets core pool size.
     *
     * @return the core pool size
     */
    int getCorePoolSize();

    /**
     * Consume message directly consume message directly result.
     *
     * @param msg        the msg
     * @param brokerName the broker name
     * @return the consume message directly result
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(final ExtMessage msg, final String brokerName);

    /**
     * Submit consume request.
     *
     * @param msgs             the msgs
     * @param processQueue     the process queue
     * @param messageQueue     the message queue
     * @param disPathToConsume the dis path to consume
     */
    void submitConsumeRequest(
            final List<ExtMessage> msgs,
            final ProcessQueue processQueue,
            final MessageQueue messageQueue,
            final boolean disPathToConsume);
}
