package cn.xianyijun.wisp.common.protocol.body;

import lombok.Data;

@Data
public class ConsumeQueueData {

    private long physicOffset;
    private int physicSize;
    private long tagsCode;
    private String extendDataJson;
    private String bitMap;
    private boolean eval;
    private String msg;
}
