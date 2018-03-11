package cn.xianyijun.demo;

import cn.xianyijun.wisp.client.consumer.DefaultPushConsumer;
import cn.xianyijun.wisp.client.consumer.listener.ConsumeConcurrentlyStatus;
import cn.xianyijun.wisp.client.consumer.listener.MessageListenerConcurrently;
import cn.xianyijun.wisp.common.consumer.ConsumeWhereEnum;
import cn.xianyijun.wisp.exception.ClientException;

public class PushConsumer {

    public static void main(String[] args) {
        DefaultPushConsumer consumer = new DefaultPushConsumer("please_rename_unique_group_name_4");

        consumer.setConsumeFromWhere(ConsumeWhereEnum.CONSUME_FROM_FIRST_OFFSET);

        consumer.setNameServerAddr("localhost:9876");
        try {
            consumer.subscribe("TopicTest", "*");

            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });

            consumer.start();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
