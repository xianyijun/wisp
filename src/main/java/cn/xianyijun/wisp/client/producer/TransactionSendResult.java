package cn.xianyijun.wisp.client.producer;

import lombok.Data;

/**
 * @author xianyijun
 */
@Data
public class TransactionSendResult extends SendResult{

    private LocalTransactionState localTransactionState;
}
