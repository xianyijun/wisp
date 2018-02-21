package cn.xianyijun.wisp.common;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * The type Config manager.
 * @author xianyijun
 */
@Slf4j
public abstract class AbstractConfigManager {

    /**
     * Load boolean.
     *
     * @return the boolean
     */
    public boolean load() {
        String fileName = null;
        try {
            fileName = this.configFilePath();
            String jsonString = MixAll.file2String(fileName);

            if (null == jsonString || jsonString.length() == 0) {
                return this.loadBak();
            } else {
                this.decode(jsonString);
                log.info("load {} OK", fileName);
                return true;
            }
        } catch (Exception e) {
            log.error("load [{}] failed, and try to load backup file", fileName, e);
            return this.loadBak();
        }
    }

    private boolean loadBak() {
        String fileName = null;
        try {
            fileName = this.configFilePath();
            String jsonString = MixAll.file2String(fileName + ".bak");
            if (jsonString != null && jsonString.length() > 0) {
                this.decode(jsonString);
                log.info("load [{}] OK", fileName);
                return true;
            }
        } catch (Exception e) {
            log.error("load [{}] Failed", fileName, e);
            return false;
        }

        return true;
    }

    /**
     * Persist.
     */
    public synchronized void persist() {
        String jsonString = this.encode(true);
        if (jsonString != null) {
            String fileName = this.configFilePath();
            try {
                MixAll.string2File(jsonString, fileName);
            } catch (IOException e) {
                log.error("persist file [{}] exception", fileName, e);
            }
        }
    }


    /**
     * Config file path string.
     *
     * @return the string
     */
    public abstract String configFilePath();

    /**
     * Decode.
     *
     * @param jsonString the json string
     */
    public abstract void decode(final String jsonString);

    /**
     * Encode string.
     *
     * @return the string
     */
    public abstract String encode();

    /**
     * Encode string.
     *
     * @param prettyFormat the pretty format
     * @return the string
     */
    public abstract String encode(final boolean prettyFormat);
}