package cn.xianyijun.wisp.client.producer;

public enum LocalTransactionState {
    COMMIT_MESSAGE,
    ROLLBACK_MESSAGE,
    UNKNOW,
}
