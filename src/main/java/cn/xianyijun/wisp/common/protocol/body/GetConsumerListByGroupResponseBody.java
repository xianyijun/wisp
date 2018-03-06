package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetConsumerListByGroupResponseBody extends RemotingSerializable {
    private List<String> consumerIdList;
}
