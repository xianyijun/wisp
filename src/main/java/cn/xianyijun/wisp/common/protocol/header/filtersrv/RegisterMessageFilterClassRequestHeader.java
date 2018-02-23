package cn.xianyijun.wisp.common.protocol.header.filtersrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class RegisterMessageFilterClassRequestHeader implements CommandCustomHeader {
    private String consumerGroup;
    private String topic;
    private String className;
    private Integer classCRC;
}
