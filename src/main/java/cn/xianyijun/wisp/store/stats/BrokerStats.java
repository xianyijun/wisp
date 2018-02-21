package cn.xianyijun.wisp.store.stats;

import cn.xianyijun.wisp.store.DefaultMessageStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
public class BrokerStats {

    private final DefaultMessageStore defaultMessageStore;

    private volatile long msgPutTotalYesterdayMorning;

    private volatile long msgPutTotalTodayMorning;

    private volatile long msgGetTotalYesterdayMorning;

    private volatile long msgGetTotalTodayMorning;

    public void record() {
        this.msgPutTotalYesterdayMorning = this.msgPutTotalTodayMorning;
        this.msgGetTotalYesterdayMorning = this.msgGetTotalTodayMorning;

        this.msgPutTotalTodayMorning =
                this.defaultMessageStore.getStoreStatsService().getPutMessageTimesTotal();
        this.msgGetTotalTodayMorning =
                this.defaultMessageStore.getStoreStatsService().getGetMessageTransferedMsgCount().get();

        log.info("yesterday put message total: {}", msgPutTotalTodayMorning - msgPutTotalYesterdayMorning);
        log.info("yesterday get message total: {}", msgGetTotalTodayMorning - msgGetTotalYesterdayMorning);
    }
}
