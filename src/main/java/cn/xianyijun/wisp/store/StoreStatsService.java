package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.ServiceThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Getter
public class StoreStatsService extends ServiceThread {

    private static final int FREQUENCY_OF_SAMPLING = 1000;

    private static final int MAX_RECORDS_OF_SAMPLING = 60 * 10;
    private static final String[] PUT_MESSAGE_ENTIRE_TIME_MAX_DESC = new String[]{
            "[<=0ms]", "[0~10ms]", "[10~50ms]", "[50~100ms]", "[100~200ms]", "[200~500ms]", "[500ms~1s]", "[1~2s]", "[2~3s]", "[3~4s]", "[4~5s]", "[5~10s]", "[10s~]",
    };


    private static int printTPSInterval = 60;

    private final AtomicLong putMessageFailedTimes = new AtomicLong(0);

    private final Map<String, AtomicLong> putMessageTopicTimesTotal =
            new ConcurrentHashMap<>(128);
    private final Map<String, AtomicLong> putMessageTopicSizeTotal =
            new ConcurrentHashMap<>(128);
    private final AtomicLong getMessageTransferedMsgCount = new AtomicLong(0);
    private volatile AtomicLong[] putMessageDistributeTime;

    public StoreStatsService() {
        this.initPutMessageDistributeTime();
    }

    private AtomicLong[] initPutMessageDistributeTime() {
        AtomicLong[] next = new AtomicLong[13];
        for (int i = 0; i < next.length; i++) {
            next[i] = new AtomicLong(0);
        }

        AtomicLong[] old = this.putMessageDistributeTime;

        this.putMessageDistributeTime = next;

        return old;
    }


    public long getPutMessageTimesTotal() {
        long rs = 0;
        for (AtomicLong data : putMessageTopicTimesTotal.values()) {
            rs += data.get();
        }
        return rs;
    }

    @Override
    public String getServiceName() {
        return StoreStatsService.class.getSimpleName();
    }

    @Override
    public void run() {

    }


    public AtomicLong getSinglePutMessageTopicTimesTotal(String topic) {
        AtomicLong rs = putMessageTopicTimesTotal.get(topic);
        if (null == rs) {
            rs = new AtomicLong(0);
            putMessageTopicTimesTotal.put(topic, rs);
        }
        return rs;
    }


    public AtomicLong getSinglePutMessageTopicSizeTotal(String topic) {
        AtomicLong rs = putMessageTopicSizeTotal.get(topic);
        if (null == rs) {
            rs = new AtomicLong(0);
            putMessageTopicSizeTotal.put(topic, rs);
        }
        return rs;
    }
}
