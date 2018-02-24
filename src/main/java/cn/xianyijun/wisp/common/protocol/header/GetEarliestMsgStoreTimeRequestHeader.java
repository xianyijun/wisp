package cn.xianyijun.wisp.common.protocol.header;

import cn.xianyijun.wisp.remoting.CommandCustomHeader;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class GetEarliestMsgStoreTimeRequestHeader implements CommandCustomHeader{

    private String topic;

    private Integer queueId;

}
