package cn.xianyijun.wisp.client;


import cn.xianyijun.wisp.client.producer.factory.ClientFactory;
import cn.xianyijun.wisp.remoting.RPCHook;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xianyijun
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class MQClientManager {

    private static MQClientManager instance = new MQClientManager();

    private AtomicInteger factoryIndexGenerator = new AtomicInteger();

    private ConcurrentMap<String, ClientFactory> factoryTable =
            new ConcurrentHashMap<>();


    public static MQClientManager getInstance() {
        return instance;
    }

    public ClientFactory getAndCreateMQClientInstance(final ClientConfig clientConfig, RPCHook rpcHook) {
        String clientId = clientConfig.buildMQClientId();
        ClientFactory instance = this.factoryTable.get(clientId);
        if (null == instance) {
            instance =
                    new ClientFactory(clientConfig.cloneClientConfig(),
                            this.factoryIndexGenerator.getAndIncrement(), clientId, rpcHook);
            ClientFactory prev = this.factoryTable.putIfAbsent(clientId, instance);
            if (prev != null) {
                instance = prev;
                log.warn("Returned Previous MQClientInstance for clientId:[{}]", clientId);
            } else {
                log.info("Created new MQClientInstance for clientId:[{}]", clientId);
            }
        }

        return instance;
    }

    public void removeClientFactory(final String clientId) {
        this.factoryTable.remove(clientId);
    }
}
