package cn.xianyijun.demo;

import cn.xianyijun.wisp.client.producer.DefaultProducer;
import cn.xianyijun.wisp.client.producer.SendResult;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.exception.ClientException;

public class Producer {
    public static void main(String[] args) throws ClientException, InterruptedException {
        System.setProperty(MixAll.NAME_SERVER_ADDR_PROPERTY,"localhost:9876");
        /*
         * Instantiate with a producer group name.
         */
        DefaultProducer producer = new DefaultProducer("please_rename_unique_group_name");

        producer.start();

        try {

            Message msg = new Message("TopicTest" /* Topic */,
                    "TagA" /* Tag */,
                    ("Hello RocketMQ ").getBytes(MixAll.DEFAULT_CHARSET) /* Message body */
            );

            SendResult sendResult = producer.send(msg);

            System.out.printf("%s%n", sendResult);
        } catch (Exception e) {
            e.printStackTrace();
            Thread.sleep(1000);
        }

        /*
         * Shut down once the producer instance is not longer in use.
         */
//        producer.shutdown();
    }

}
