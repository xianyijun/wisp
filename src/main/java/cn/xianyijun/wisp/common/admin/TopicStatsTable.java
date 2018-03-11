package cn.xianyijun.wisp.common.admin;

import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashMap;

/**
 * @author xianyijun
 */
@Data
public class TopicStatsTable extends RemotingSerializable {

    private HashMap<MessageQueue, TopicOffset> offsetTable = new HashMap<>();
}
