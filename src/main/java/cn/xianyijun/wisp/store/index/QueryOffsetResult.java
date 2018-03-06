package cn.xianyijun.wisp.store.index;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
public class QueryOffsetResult {
    private final List<Long> phyOffsets;
    private final long indexLastUpdateTimestamp;
    private final long indexLastUpdatePhyoffSet;
}
