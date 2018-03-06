package cn.xianyijun.wisp.client.hook;

import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;

import java.util.List;

@Data
public class FilterMessageContext {
    private String consumerGroup;
    private List<ExtMessage> msgList;
    private MessageQueue mq;
    private Object arg;
    private boolean unitMode;
}
