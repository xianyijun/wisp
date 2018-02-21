package cn.xianyijun.wisp.broker.filter;

import cn.xianyijun.wisp.common.BrokerConfig;
import cn.xianyijun.wisp.store.CommitLogDispatcher;
import cn.xianyijun.wisp.store.DispatchRequest;
import lombok.RequiredArgsConstructor;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
public class CommitLogDispatcherCalcBitMap implements CommitLogDispatcher {

    protected final BrokerConfig brokerConfig;
    protected final ConsumerFilterManager consumerFilterManager;


    @Override
    public void dispatch(DispatchRequest request) {

    }
}
