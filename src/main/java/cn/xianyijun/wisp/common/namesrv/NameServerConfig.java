package cn.xianyijun.wisp.common.namesrv;

import cn.xianyijun.wisp.common.MixAll;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
@Setter
public class NameServerConfig {

    private String wispHome = System.getProperty(MixAll.WISP_HOME_PROPERTY, System.getenv(MixAll.WISP_HOME_ENV));
    private String kvConfigPath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "kvConfig.json";
    private String configStorePath = System.getProperty("user.home") + File.separator + "namesrv" + File.separator + "namesrv.properties";
    private String productEnvName = "center";
    private boolean cluster = false;
    private boolean orderMessageEnable = false;
}
