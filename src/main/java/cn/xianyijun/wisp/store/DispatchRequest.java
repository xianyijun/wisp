package cn.xianyijun.wisp.store;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
public class DispatchRequest {

    private final String topic;
    private final int queueId;
    private final long commitLogOffset;
    private final int msgSize;
    private final long tagsCode;
    private final long storeTimestamp;
    private final long consumeQueueOffset;
    private final String keys;
    private final boolean success;
    private final String uniqueKey;

    private final int sysFlag;
    private final long preparedTransactionOffset;
    private final Map<String, String> propertiesMap;
    private byte[] bitMap;

    public DispatchRequest(int size, boolean success) {
        this.topic = "";
        this.queueId = 0;
        this.commitLogOffset = 0;
        this.msgSize = size;
        this.tagsCode = 0;
        this.storeTimestamp = 0;
        this.consumeQueueOffset = 0;
        this.keys = "";
        this.uniqueKey = null;
        this.sysFlag = 0;
        this.preparedTransactionOffset = 0;
        this.success = success;
        this.propertiesMap = null;
    }

    public DispatchRequest(
            final String topic,
            final int queueId,
            final long commitLogOffset,
            final int msgSize,
            final long tagsCode,
            final long storeTimestamp,
            final long consumeQueueOffset,
            final String keys,
            final String uniqueKey,
            final int sysFlag,
            final long preparedTransactionOffset,
            final Map<String, String> propertiesMap
    ) {
        this.topic = topic;
        this.queueId = queueId;
        this.commitLogOffset = commitLogOffset;
        this.msgSize = msgSize;
        this.tagsCode = tagsCode;
        this.storeTimestamp = storeTimestamp;
        this.consumeQueueOffset = consumeQueueOffset;
        this.keys = keys;
        this.uniqueKey = uniqueKey;

        this.sysFlag = sysFlag;
        this.preparedTransactionOffset = preparedTransactionOffset;
        this.success = true;
        this.propertiesMap = propertiesMap;
    }

}
