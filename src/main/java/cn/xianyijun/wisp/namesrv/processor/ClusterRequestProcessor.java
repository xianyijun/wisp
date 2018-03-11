package cn.xianyijun.wisp.namesrv.processor;

import cn.xianyijun.wisp.common.namesrv.NameServerUtils;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.namesrv.NameServerController;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.tools.admin.DefaultMQExtAdmin;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@Slf4j
public class ClusterRequestProcessor extends DefaultRequestProcessor {

    private final String productEnvName;

    private final DefaultMQExtAdmin extAdmin;

    public ClusterRequestProcessor(NameServerController nameServerController, String productEnvName) {
        super(nameServerController);
        this.productEnvName = productEnvName;

        extAdmin = new DefaultMQExtAdmin();

        extAdmin.setInstanceName("CLUSTER_NS_INS_" + productEnvName);
        extAdmin.setUnitName(productEnvName);

        try {
            extAdmin.start();
        } catch (ClientException e) {
            log.error("Failed to start processor", e);
        }
    }

    @Override
    protected RemotingCommand getRouteInfoByTopic(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final GetRouteInfoRequestHeader requestHeader =
                (GetRouteInfoRequestHeader) request.decodeCommandCustomHeader(GetRouteInfoRequestHeader.class);

        TopicRouteData topicRouteData = this.nameServerController.getRouteInfoManager().pickupTopicRouteData(requestHeader.getTopic());
        if (topicRouteData != null) {
            String orderTopicConf =
                    this.nameServerController.getKvConfigManager().getKVConfig(NameServerUtils.NAMESPACE_ORDER_TOPIC_CONFIG,
                            requestHeader.getTopic());
            topicRouteData.setOrderTopicConf(orderTopicConf);
        } else {
            try {
                topicRouteData = extAdmin.examineTopicRouteInfo(requestHeader.getTopic());
            } catch (Exception e) {
                log.info("get route info by topic from product environment failed. envName={},", productEnvName);
            }
        }

        if (topicRouteData != null) {
            byte[] content = topicRouteData.encode();
            response.setBody(content);
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);
            return response;
        }

        response.setCode(ResponseCode.TOPIC_NOT_EXIST);
        response.setRemark("No topic route info in name server for the topic: " + requestHeader.getTopic());
        return response;
    }
}
