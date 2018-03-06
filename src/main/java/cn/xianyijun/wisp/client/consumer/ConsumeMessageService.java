package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.protocol.body.ConsumeMessageDirectlyResult;

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
     * Consume message directly consume message directly result.
     *
     * @param msg        the msg
     * @param brokerName the broker name
     * @return the consume message directly result
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(final ExtMessage msg, final String brokerName);

}
