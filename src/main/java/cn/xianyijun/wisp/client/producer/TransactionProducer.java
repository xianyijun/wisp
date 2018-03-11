package cn.xianyijun.wisp.client.producer;

import cn.xianyijun.wisp.common.message.Message;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.Getter;
import lombok.Setter;

/**
 * @author xianyijun
 */
@Getter
@Setter
public class TransactionProducer extends DefaultProducer{

    private TransactionCheckListener transactionCheckListener;
    private int checkThreadPoolMinSize = 1;
    private int checkThreadPoolMaxSize = 1;
    private int checkRequestHoldMax = 2000;

    public TransactionProducer(final String producerGroup) {
        super(producerGroup);
    }

    public TransactionProducer(final String producerGroup, RPCHook rpcHook) {
        super(producerGroup, rpcHook);
    }


    @Override
    public void start() throws ClientException {
        this.producerDelegate.initTransactionEnv();
        super.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.producerDelegate.destroyTransactionEnv();
    }

    @Override
    public TransactionSendResult sendMessageInTransaction(Message msg, LocalTransactionExecutor transactionExecutor, Object arg) throws ClientException {
        if (null == this.transactionCheckListener) {
            throw new ClientException("localTransactionBranchCheckListener is null", null);
        }
        return this.producerDelegate.sendMessageInTransaction(msg, transactionExecutor, arg);
    }
}
