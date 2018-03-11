package cn.xianyijun.wisp.tools.admin.api;

import lombok.Data;

@Data
public class MessageTrack {
    private String consumerGroup;
    private TrackType trackType;
    private String exceptionDesc;
}
