package cn.xianyijun.wisp.client;

import cn.xianyijun.wisp.common.message.ExtMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Getter
@ToString
public class QueryResult {
    private final long indexLastUpdateTimestamp;
    private final List<ExtMessage> messageList;
}
