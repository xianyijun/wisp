package cn.xianyijun.wisp.common.protocol.body;

public enum ConsumeResultEnum {
    CR_SUCCESS,
    CR_LATER,
    CR_ROLLBACK,
    CR_COMMIT,
    CR_THROW_EXCEPTION,
    CR_RETURN_NULL,
}
