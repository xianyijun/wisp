package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;

public class ConsumeMessageDirectlyResult extends RemotingSerializable {
    private boolean order = false;
    private boolean autoCommit = true;
    private ConsumeResultEnum consumeResult;
    private String remark;
    private long spentTimeMills;
}
