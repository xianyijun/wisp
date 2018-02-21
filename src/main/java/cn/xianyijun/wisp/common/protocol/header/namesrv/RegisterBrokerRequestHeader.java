package cn.xianyijun.wisp.common.protocol.header.namesrv;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

@Data
public class RegisterBrokerRequestHeader implements CommandCustomHeader {

    private String brokerName;
    private String brokerAddr;
    private String clusterName;
    private String haServerAddr;
    private Long brokerId;
}
