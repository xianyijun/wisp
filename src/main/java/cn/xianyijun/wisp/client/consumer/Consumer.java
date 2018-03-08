package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.MQAdmin;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;

/**
 * The interface Consumer.
 *
 * @author xianyijun
 */
public interface Consumer extends MQAdmin {

    /**
     * Send message back.
     *
     * @param msg        the msg
     * @param delayLevel the delay level
     * @param brokerName the broker name
     * @throws RemotingException    the remoting exception
     * @throws BrokerException      the broker exception
     * @throws InterruptedException the interrupted exception
     * @throws ClientException      the client exception
     */
    void sendMessageBack(final ExtMessage msg, final int delayLevel, final String brokerName)
            throws RemotingException, BrokerException, InterruptedException, ClientException;

}
