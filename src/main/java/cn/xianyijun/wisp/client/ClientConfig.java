package cn.xianyijun.wisp.client;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.remoting.netty.TlsSystemConfig;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author xianyijun
 */
@Data
public class ClientConfig {
    public static final String SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY = "wisp.sendMessageWithVIPChannel";

    private String nameServerAddr = System.getProperty(MixAll.NAME_SERVER_ADDR_PROPERTY, System.getenv(MixAll.NAME_SERVER_ADDR_ENV));
    private String clientIP = RemotingUtils.getLocalAddress();
    private String instanceName = System.getProperty("wisp.client.name", "DEFAULT");

    private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();


    private int pollNameServerInterval = 1000 * 30;
    private int heartbeatBrokerInterval = 1000 * 30;
    private int persistConsumerOffsetInterval = 1000 * 5;
    private boolean unitMode = false;
    private String unitName;
    private boolean vipChannelEnabled = Boolean.parseBoolean(System.getProperty(SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY, "true"));

    private boolean useTLS = TlsSystemConfig.tlsEnable;

    public void changeInstanceNameToPID() {
        if ("DEFAULT".equals(this.instanceName)) {
            this.instanceName = String.valueOf(UtilAll.getPid());
        }
    }


    public String buildMQClientId() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClientIP());

        sb.append("@");
        sb.append(this.getInstanceName());
        if (!StringUtils.isEmpty(this.unitName)) {
            sb.append("@");
            sb.append(this.unitName);
        }

        return sb.toString();
    }

    public ClientConfig cloneClientConfig() {
        ClientConfig cc = new ClientConfig();
        cc.nameServerAddr = nameServerAddr;
        cc.clientIP = clientIP;
        cc.instanceName = instanceName;
        cc.clientCallbackExecutorThreads = clientCallbackExecutorThreads;
        cc.pollNameServerInterval = pollNameServerInterval;
        cc.heartbeatBrokerInterval = heartbeatBrokerInterval;
        cc.persistConsumerOffsetInterval = persistConsumerOffsetInterval;
        cc.unitMode = unitMode;
        cc.unitName = unitName;
        cc.vipChannelEnabled = vipChannelEnabled;
        cc.useTLS = useTLS;
        return cc;
    }

    public void resetClientConfig(final ClientConfig cc) {
        this.nameServerAddr = cc.nameServerAddr;
        this.clientIP = cc.clientIP;
        this.instanceName = cc.instanceName;
        this.clientCallbackExecutorThreads = cc.clientCallbackExecutorThreads;
        this.pollNameServerInterval = cc.pollNameServerInterval;
        this.heartbeatBrokerInterval = cc.heartbeatBrokerInterval;
        this.persistConsumerOffsetInterval = cc.persistConsumerOffsetInterval;
        this.unitMode = cc.unitMode;
        this.unitName = cc.unitName;
        this.vipChannelEnabled = cc.vipChannelEnabled;
        this.useTLS = cc.useTLS;
    }
}
