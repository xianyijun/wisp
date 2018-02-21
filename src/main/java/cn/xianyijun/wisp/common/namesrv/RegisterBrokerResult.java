package cn.xianyijun.wisp.common.namesrv;

import cn.xianyijun.wisp.common.protocol.body.KVTable;
import lombok.Data;

@Data
public class RegisterBrokerResult {
    private String haServerAddr;
    private String masterAddr;
    private KVTable kvTable;
}
