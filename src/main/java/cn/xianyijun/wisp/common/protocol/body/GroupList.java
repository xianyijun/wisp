package cn.xianyijun.wisp.common.protocol.body;

import cn.xianyijun.wisp.common.protocol.RemotingSerializable;
import lombok.Data;

import java.util.HashSet;

@Data
public class GroupList extends RemotingSerializable {
    private HashSet<String> groupList = new HashSet<>();
}
