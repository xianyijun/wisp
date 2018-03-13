package cn.xianyijun.wisp.store.result;

import cn.xianyijun.wisp.store.status.PutMessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author xianyijun
 */
@Data
@AllArgsConstructor
public class PutMessageResult {
    private PutMessageStatus putMessageStatus;
    private AppendMessageResult appendMessageResult;


    public boolean isOk() {
        return this.appendMessageResult != null && this.appendMessageResult.isOk();
    }
}
