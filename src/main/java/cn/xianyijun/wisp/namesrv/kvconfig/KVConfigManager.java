package cn.xianyijun.wisp.namesrv.kvconfig;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.namesrv.NameServerController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class KVConfigManager {

    private final NameServerController nameServerController;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final HashMap<String, HashMap<String, String>> configTable = new HashMap<>();

    public void load() {
        String content = null;
        try {
            content = MixAll.file2String(this.nameServerController.getNameServerConfig().getKvConfigPath());
        } catch (IOException e) {
            log.warn("Load KV config table exception", e);
        }
        if (content != null) {
            KVConfigSerializeWrapper kvConfigSerializeWrapper =
                    KVConfigSerializeWrapper.fromJson(content, KVConfigSerializeWrapper.class);
            if (null != kvConfigSerializeWrapper) {
                this.configTable.putAll(kvConfigSerializeWrapper.getConfigTable());
                log.info("load KV config table OK");
            }
        }
    }


    public void printAllPeriodically() {
        try {
            this.lock.readLock().lockInterruptibly();
            try {
                log.info("configTable SIZE: {}", this.configTable.size());
                for (Map.Entry<String, HashMap<String, String>> next : this.configTable.entrySet()) {
                    for (Map.Entry<String, String> nextSub : next.getValue().entrySet()) {
                        log.info("configTable NS: {} Key: {} Value: {}", next.getKey(), nextSub.getKey(),
                                nextSub.getValue());
                    }
                }
            } finally {
                this.lock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            log.error("printAllPeriodically InterruptedException", e);
        }
    }

}
