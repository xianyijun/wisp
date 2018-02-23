package cn.xianyijun.wisp.common.protocol.heartbeat;

import lombok.Data;
import lombok.ToString;

/**
 * @author xianyijun
 */
@Data
@ToString
public class ProducerData {
    private String groupName;
}
