package cn.xianyijun.wisp.common.message;

import lombok.Data;

import java.net.SocketAddress;

/**
 * @author xianyijun
 */
@Data
public class ExtMessage extends Message {

    private int queueId;

    private int sysFlag;

    private int storeSize;

    private long queueOffset;
    private long bornTimestamp;
    private SocketAddress bornHost;

    private long storeTimestamp;
    private SocketAddress storeHost;
    private String msgId;
    private long commitLogOffset;
    private int bodyCRC;
    private int reConsumeTimes;

    private long preparedTransactionOffset;
}
