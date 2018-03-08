package cn.xianyijun.wisp.common.namesrv;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.utils.HttpTinyClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class TopAddressing {

    private String nameServerAddr;
    private String wsAddr;
    private String unitName;

    public TopAddressing(final String wsAddr) {
        this(wsAddr, null);
    }

    public TopAddressing(final String wsAddr, final String unitName) {
        this.wsAddr = wsAddr;
        this.unitName = unitName;
    }

    private static String clearNewLine(final String str) {
        String newString = str.trim();
        int index = newString.indexOf("\r");
        if (index != -1) {
            return newString.substring(0, index);
        }

        index = newString.indexOf("\n");
        if (index != -1) {
            return newString.substring(0, index);
        }

        return newString;
    }

    public final String fetchNameServerAddr() {
        return fetchNameServerAddr(true, 3000);
    }

    private final String fetchNameServerAddr(boolean verbose, long timeoutMills) {
        String url = this.wsAddr;
        try {
            if (!StringUtils.isBlank(this.unitName)) {
                url = url + "-" + this.unitName + "?nofix=1";
            }
            HttpTinyClient.HttpResult result = HttpTinyClient.httpGet(url, null, null, "UTF-8", timeoutMills);
            if (HttpTinyClient.STATUS_OK == result.code) {
                String responseStr = result.content;
                if (responseStr != null) {
                    return clearNewLine(responseStr);
                } else {
                    log.error("fetch nameServer address is null");
                }
            } else {
                log.error("fetch nameServer address failed. statusCode={}", result.code);
            }
        } catch (IOException e) {
            if (verbose) {
                log.error("fetch name server address exception", e);
            }
        }

        if (verbose) {
            String errorMsg =
                    "connect to " + url + " failed, maybe the domain name " + MixAll.getWSAddr() + " not bind in /etc/hosts";
            log.warn(errorMsg);
        }
        return MixAll.DEFAULT_NAME_SERVER_ADDR_LOOKUP;
    }

}
