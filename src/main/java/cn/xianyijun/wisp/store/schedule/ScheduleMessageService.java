package cn.xianyijun.wisp.store.schedule;

import cn.xianyijun.wisp.common.AbstractConfigManager;
import cn.xianyijun.wisp.store.DefaultMessageStore;
import cn.xianyijun.wisp.store.config.StorePathConfigHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
@Getter
public class ScheduleMessageService extends AbstractConfigManager {

    public static final String SCHEDULE_TOPIC = "SCHEDULE_TOPIC_XXXX";
    private final DefaultMessageStore defaultMessageStore;
    private final ConcurrentMap<Integer, Long> delayLevelTable =
            new ConcurrentHashMap<>(32);
    @Setter
    private int maxDelayLevel;

    @Override
    public String configFilePath() {
        return StorePathConfigHelper.getDelayOffsetStorePath(this.defaultMessageStore.getMessageStoreConfig()
                .getStorePathRootDir());
    }

    @Override
    public void decode(String jsonString) {

    }

    @Override
    public String encode() {
        return null;
    }

    @Override
    public String encode(boolean prettyFormat) {
        return null;
    }


    public long computeDeliverTimestamp(final int delayLevel, final long storeTimestamp) {
        Long time = this.delayLevelTable.get(delayLevel);
        if (time != null) {
            return time + storeTimestamp;
        }

        return storeTimestamp + 1000;
    }
}
