package cn.xianyijun.wisp.client.hook;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.producer.SendResult;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;

@Data
public class CheckForbiddenContext {

    private String nameServerAddr;
    private String group;
    private Message message;
    private MessageQueue mq;
    private String brokerAddr;
    private CommunicationMode communicationMode;
    private SendResult sendResult;
    private Exception exception;
    private Object arg;
    private boolean unitMode = false;
}
