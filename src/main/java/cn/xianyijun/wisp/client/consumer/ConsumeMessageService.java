package cn.xianyijun.wisp.client.consumer;

/**
 * The interface Consume message service.
 * @author xianyijun
 */
public interface ConsumeMessageService {


    /**
     * Inc core pool size.
     */
    void incCorePoolSize();

    /**
     * Dec core pool size.
     */
    void decCorePoolSize();
}
