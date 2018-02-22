package cn.xianyijun.wisp.common;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class Configuration {

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private List<Object> configObjectList = new ArrayList<>(4);

    private String storePath;
    private boolean storePathFromConfig = false;
    private Object storePathObject;
    private Field storePathField;
    private DataVersion dataVersion = new DataVersion();

    private Properties allConfigs = new Properties();

    public Configuration(String storePath, Object... configObjects) {
        this(configObjects);
        this.storePath = storePath;
    }

    public Configuration(Object... configObjects) {
        if (configObjects == null || configObjects.length == 0) {
            return;
        }
        for (Object configObject : configObjects) {
            registerConfig(configObject);
        }
    }

    public void registerConfig(Object configObject) {
        try {
            readWriteLock.writeLock().lockInterruptibly();
            try {
                Properties registerProps = MixAll.object2Properties(configObject);

                merge(registerProps, this.allConfigs);

                configObjectList.add(configObject);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            log.error("[Configuration] registerConfig failure , ex: {}", e);
        }
    }

    public void registerConfig(Properties config) {
        if (config == null) {
            return;
        }
        try {
            readWriteLock.writeLock().lockInterruptibly();
            try {
                merge(config, this.allConfigs);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            log.error("[Configuration] registerConfig failure , ex: {}", e);
        }
    }


    private void merge(Properties from, Properties to) {
        for (Object key : from.keySet()) {
            Object fromObj = from.get(key), toObj = to.get(key);
            if (toObj != null && !toObj.equals(fromObj)) {
                log.info("Replace, key: {}, value: {} -> {}", key, toObj, fromObj);
            }
            to.put(key, fromObj);
        }
    }

    public void setStorePathFromConfig(Object object, String fieldName) {
        try {
            readWriteLock.writeLock().lockInterruptibly();

            try {
                this.storePathFromConfig = true;
                this.storePathObject = object;
                this.storePathField = object.getClass().getDeclaredField(fieldName);
                assert this.storePathField != null
                        && !Modifier.isStatic(this.storePathField.getModifiers());
                this.storePathField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            log.error("setStorePathFromConfig lock error");
        }
    }

}
