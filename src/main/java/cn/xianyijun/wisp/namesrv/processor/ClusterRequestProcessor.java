package cn.xianyijun.wisp.namesrv.processor;

import cn.xianyijun.wisp.namesrv.NameServerController;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@Slf4j
public class ClusterRequestProcessor extends DefaultRequestProcessor {

    private final String productEnvName;

    public ClusterRequestProcessor(NameServerController nameServerController, String productEnvName) {
        super(nameServerController);
        this.productEnvName = productEnvName;
    }

}
