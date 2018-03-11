package cn.xianyijun.wisp.remoting;

/**
 * The interface Remoting service.
 *
 * @author xianyijun
 */
public interface RemotingService {
    /**
     * Start.
     */
    void start();

    /**
     * Shutdown.
     */
    void shutdown();

    /**
     * Register rpc hook.
     *
     * @param rpcHook the rpc hook
     */
    void registerRPCHook(RPCHook rpcHook);
}
