package cn.xianyijun.wisp.common;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DataVersion extends RemotingSerializable {
    private long timestamp = System.currentTimeMillis();
    private AtomicLong counter = new AtomicLong(0);

    public void assignNewOne(final DataVersion dataVersion) {
        this.timestamp = dataVersion.timestamp;
        this.counter.set(dataVersion.counter.get());
    }

    public void nextVersion() {
        this.timestamp = System.currentTimeMillis();
        this.counter.incrementAndGet();
    }
}

