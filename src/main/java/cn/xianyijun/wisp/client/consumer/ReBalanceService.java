package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.ServiceThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReBalanceService extends ServiceThread {

    private final ClientInstance clientFactory;
    @Override
    public String getServiceName() {
        return ReBalanceService.class.getSimpleName();
    }

    @Override
    public void run() {

    }
}
