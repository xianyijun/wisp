package cn.xianyijun.wisp.client.hook;

import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageQueue;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author xianyijun
 */
@Data
public class ConsumeMessageContext {
    private String consumerGroup;
    private List<ExtMessage> msgList;
    private MessageQueue mq;
    private boolean success;
    private String status;
    private Object mqTraceContext;
    private Map<String, String> props;
}
