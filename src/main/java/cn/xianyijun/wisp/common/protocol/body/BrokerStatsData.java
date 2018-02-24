package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

@Data
public class BrokerStatsData extends RemotingSerializable {

    private BrokerStatsItem statsMinute;

    private BrokerStatsItem statsHour;

    private BrokerStatsItem statsDay;
}
