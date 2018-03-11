package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageQueue;

import java.util.List;

/**
 * The interface Message queue selector.
 * @author xianyijun
 */
public interface MessageQueueSelector {

    /**
     * Select message queue.
     *
     * @param mqs the mqs
     * @param msg the msg
     * @param arg the arg
     * @return the message queue
     */
    MessageQueue select(final List<MessageQueue> mqs, final Message msg, final Object arg);
}
