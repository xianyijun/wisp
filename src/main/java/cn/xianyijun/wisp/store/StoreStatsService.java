package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.common.ServiceThread;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

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

    private ReentrantLock lockPut = new ReentrantLock();
    private volatile long putMessageEntireTimeMax = 0;

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

    public void setPutMessageEntireTimeMax(long value) {
        final AtomicLong[] times = this.putMessageDistributeTime;

        if (null == times) {
            return;
        }

        // us
        if (value <= 0) {
            times[0].incrementAndGet();
        } else if (value < 10) {
            times[1].incrementAndGet();
        } else if (value < 50) {
            times[2].incrementAndGet();
        } else if (value < 100) {
            times[3].incrementAndGet();
        } else if (value < 200) {
            times[4].incrementAndGet();
        } else if (value < 500) {
            times[5].incrementAndGet();
        } else if (value < 1000) {
            times[6].incrementAndGet();
        }
        // 2s
        else if (value < 2000) {
            times[7].incrementAndGet();
        }
        // 3s
        else if (value < 3000) {
            times[8].incrementAndGet();
        }
        // 4s
        else if (value < 4000) {
            times[9].incrementAndGet();
        }
        // 5s
        else if (value < 5000) {
            times[10].incrementAndGet();
        }
        // 10s
        else if (value < 10000) {
            times[11].incrementAndGet();
        } else {
            times[12].incrementAndGet();
        }

        if (value > this.putMessageEntireTimeMax) {
            this.lockPut.lock();
            this.putMessageEntireTimeMax =
                    value > this.putMessageEntireTimeMax ? value : this.putMessageEntireTimeMax;
            this.lockPut.unlock();
        }
    }

}
