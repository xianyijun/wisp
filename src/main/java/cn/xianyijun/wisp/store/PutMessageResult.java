package cn.xianyijun.wisp.store;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class PutMessageResult {
    private PutMessageStatus putMessageStatus;
    private AppendMessageResult appendMessageResult;
}
