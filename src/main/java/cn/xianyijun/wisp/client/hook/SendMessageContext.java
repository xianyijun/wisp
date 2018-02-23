package cn.xianyijun.wisp.client.hook;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.producer.ProducerDelegate;
import cn.xianyijun.wisp.client.producer.SendResult;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.message.MessageType;
import lombok.Data;

import java.util.Map;

/**
 * @author xianyijun
 */
@Data
public class SendMessageContext {

    private String producerGroup;
    private Message message;
    private MessageQueue mq;
    private String brokerAddr;
    private String bornHost;
    private CommunicationMode communicationMode;
    private SendResult sendResult;
    private Exception exception;
    private Object mqTraceContext;
    private Map<String, String> props;
    private ProducerDelegate producer;
    private MessageType msgType = MessageType.NORMAL_MESSAGE;
}
