package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.ServiceThread;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PullMessageService extends ServiceThread {

    private final ClientInstance clientFactory;

    @Override
    public String getServiceName() {
        return PullMessageService.class.getSimpleName();
    }

    @Override
    public void run() {

    }
}
