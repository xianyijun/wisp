package cn.xianyijun.demo;

import cn.xianyijun.wisp.client.producer.DefaultProducer;
import cn.xianyijun.wisp.client.producer.SendCallback;
import cn.xianyijun.wisp.client.producer.SendResult;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.exception.ClientException;

public class AsyncProducer {
    public static void main(String[] args) throws ClientException {

        DefaultProducer producer = new DefaultProducer("Jodie_Daily_test");
        producer.start();
        producer.setRetryTimesWhenSendAsyncFailed(0);

        for (int i = 0; i < 100; i++) {
            try {
                final int index = i;
                Message msg = new Message("TopicTest",
                        "TagA",
                        "OrderID188",
                        "Hello world".getBytes(MixAll.DEFAULT_CHARSET));
                producer.send(msg, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        System.out.printf("%-10d OK %s %n", index, sendResult.getMsgId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        System.out.printf("%-10d Exception %s %n", index, e);
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        producer.shutdown();
    }
}
