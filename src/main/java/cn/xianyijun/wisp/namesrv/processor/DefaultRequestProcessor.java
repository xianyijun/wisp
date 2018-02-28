package cn.xianyijun.wisp.namesrv.processor;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.RemotingHelper;
import cn.xianyijun.wisp.common.namesrv.NameServerUtils;
import cn.xianyijun.wisp.common.namesrv.RegisterBrokerResult;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.body.RegisterBrokerBody;
import cn.xianyijun.wisp.common.protocol.header.namesrv.DeleteKVConfigRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.DeleteTopicInNamesrvRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetKVConfigRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetKVConfigResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetKVListByNamespaceRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.GetTopicsByClusterRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.PutKVConfigRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.RegisterBrokerRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.RegisterBrokerResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.UnRegisterBrokerRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.WipeWritePermOfBrokerRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.WipeWritePermOfBrokerResponseHeader;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.namesrv.NameServerController;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Slf4j
public class DefaultRequestProcessor implements NettyRequestProcessor {
    private final NameServerController nameServerController;

    public DefaultRequestProcessor(NameServerController nameServerController) {
        this.nameServerController = nameServerController;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        switch (request.getCode()) {
            case RequestCode.PUT_KV_CONFIG:
                return this.putKVConfig(ctx, request);
            case RequestCode.GET_KV_CONFIG:
                return this.getKVConfig(ctx, request);
            case RequestCode.DELETE_KV_CONFIG:
                return this.deleteKVConfig(ctx, request);
            case RequestCode.REGISTER_BROKER:
                return this.registerBrokerWithFilterServer(ctx, request);
            case RequestCode.UNREGISTER_BROKER:
                return this.unregisterBroker(ctx, request);
            case RequestCode.GET_ROUTEINTO_BY_TOPIC:
                return this.getRouteInfoByTopic(ctx, request);
            case RequestCode.GET_BROKER_CLUSTER_INFO:
                return this.getBrokerClusterInfo(ctx, request);
            case RequestCode.WIPE_WRITE_PERM_OF_BROKER:
                return this.wipeWritePermOfBroker(ctx, request);
            case RequestCode.GET_ALL_TOPIC_LIST_FROM_NAMESERVER:
                return getAllTopicListFromNameserver(ctx, request);
            case RequestCode.DELETE_TOPIC_IN_NAMESRV:
                return deleteTopicInNamesrv(ctx, request);
            case RequestCode.GET_KVLIST_BY_NAMESPACE:
                return this.getKVListByNamespace(ctx, request);
            case RequestCode.GET_TOPICS_BY_CLUSTER:
                return this.getTopicsByCluster(ctx, request);
            case RequestCode.GET_SYSTEM_TOPIC_LIST_FROM_NS:
                return this.getSystemTopicListFromNs(ctx, request);
            case RequestCode.GET_UNIT_TOPIC_LIST:
                return this.getUnitTopicList(ctx, request);
            case RequestCode.GET_HAS_UNIT_SUB_TOPIC_LIST:
                return this.getHasUnitSubTopicList(ctx, request);
            case RequestCode.GET_HAS_UNIT_SUB_UNUNIT_TOPIC_LIST:
                return this.getHasUnitSubUnUnitTopicList(ctx, request);
            case RequestCode.UPDATE_NAMESRV_CONFIG:
                return this.updateConfig(ctx, request);
            case RequestCode.GET_NAMESRV_CONFIG:
                return this.getConfig(ctx, request);
            default:
                break;
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }


    private RemotingCommand putKVConfig(ChannelHandlerContext ctx,
                                        RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final PutKVConfigRequestHeader requestHeader =
                (PutKVConfigRequestHeader) request.decodeCommandCustomHeader(PutKVConfigRequestHeader.class);

        this.nameServerController.getKvConfigManager().putKVConfig(
                requestHeader.getNamespace(),
                requestHeader.getKey(),
                requestHeader.getValue()
        );

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getKVConfig(ChannelHandlerContext ctx,
                                        RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(GetKVConfigResponseHeader.class);
        final GetKVConfigResponseHeader responseHeader = (GetKVConfigResponseHeader) response.getCustomHeader();
        final GetKVConfigRequestHeader requestHeader =
                (GetKVConfigRequestHeader) request.decodeCommandCustomHeader(GetKVConfigRequestHeader.class);

        String value = this.nameServerController.getKvConfigManager().getKVConfig(
                requestHeader.getNamespace(),
                requestHeader.getKey()
        );

        if (value != null) {
            responseHeader.setValue(value);
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);
            return response;
        }

        response.setCode(ResponseCode.QUERY_NOT_FOUND);
        response.setRemark("No config item, Namespace: " + requestHeader.getNamespace() + " Key: " + requestHeader.getKey());
        return response;
    }

    private RemotingCommand deleteKVConfig(ChannelHandlerContext ctx,
                                           RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final DeleteKVConfigRequestHeader requestHeader =
                (DeleteKVConfigRequestHeader) request.decodeCommandCustomHeader(DeleteKVConfigRequestHeader.class);

        this.nameServerController.getKvConfigManager().deleteKVConfig(
                requestHeader.getNamespace(),
                requestHeader.getKey()
        );

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand registerBrokerWithFilterServer(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(RegisterBrokerResponseHeader.class);
        final RegisterBrokerResponseHeader responseHeader = (RegisterBrokerResponseHeader) response.getCustomHeader();
        final RegisterBrokerRequestHeader requestHeader =
                (RegisterBrokerRequestHeader) request.decodeCommandCustomHeader(RegisterBrokerRequestHeader.class);

        RegisterBrokerBody registerBrokerBody = new RegisterBrokerBody();

        if (request.getBody() != null) {
            registerBrokerBody = RegisterBrokerBody.decode(request.getBody(), RegisterBrokerBody.class);
        } else {
            registerBrokerBody.getTopicConfigSerializeWrapper().getDataVersion().setCounter(new AtomicLong(0));
            registerBrokerBody.getTopicConfigSerializeWrapper().getDataVersion().setTimestamp(0);
        }

        RegisterBrokerResult result = this.nameServerController.getRouteInfoManager().registerBroker(
                requestHeader.getClusterName(),
                requestHeader.getBrokerAddr(),
                requestHeader.getBrokerName(),
                requestHeader.getBrokerId(),
                requestHeader.getHaServerAddr(),
                registerBrokerBody.getTopicConfigSerializeWrapper(),
                registerBrokerBody.getFilterServerList(),
                ctx.channel());

        responseHeader.setHaServerAddr(result.getHaServerAddr());
        responseHeader.setMasterAddr(result.getMasterAddr());

        byte[] jsonValue = this.nameServerController.getKvConfigManager().getKVListByNamespace(NameServerUtils.NAMESPACE_ORDER_TOPIC_CONFIG);
        response.setBody(jsonValue);

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand unregisterBroker(ChannelHandlerContext ctx,
                                             RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final UnRegisterBrokerRequestHeader requestHeader =
                (UnRegisterBrokerRequestHeader) request.decodeCommandCustomHeader(UnRegisterBrokerRequestHeader.class);

        this.nameServerController.getRouteInfoManager().unregisterBroker(
                requestHeader.getClusterName(),
                requestHeader.getBrokerAddr(),
                requestHeader.getBrokerName(),
                requestHeader.getBrokerId());

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getRouteInfoByTopic(ChannelHandlerContext ctx,
                                                RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final GetRouteInfoRequestHeader requestHeader =
                (GetRouteInfoRequestHeader) request.decodeCommandCustomHeader(GetRouteInfoRequestHeader.class);

        TopicRouteData topicRouteData = this.nameServerController.getRouteInfoManager().pickupTopicRouteData(requestHeader.getTopic());

        if (topicRouteData != null) {
            if (this.nameServerController.getNameServerConfig().isOrderMessageEnable()) {
                String orderTopicConf =
                        this.nameServerController.getKvConfigManager().getKVConfig(NameServerUtils.NAMESPACE_ORDER_TOPIC_CONFIG,
                                requestHeader.getTopic());
                topicRouteData.setOrderTopicConf(orderTopicConf);
            }

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

    private RemotingCommand getBrokerClusterInfo(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] content = this.nameServerController.getRouteInfoManager().getAllClusterInfo();
        response.setBody(content);

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand wipeWritePermOfBroker(ChannelHandlerContext ctx,
                                                  RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(WipeWritePermOfBrokerResponseHeader.class);
        final WipeWritePermOfBrokerResponseHeader responseHeader = (WipeWritePermOfBrokerResponseHeader) response.getCustomHeader();
        final WipeWritePermOfBrokerRequestHeader requestHeader =
                (WipeWritePermOfBrokerRequestHeader) request.decodeCommandCustomHeader(WipeWritePermOfBrokerRequestHeader.class);

        int wipeTopicCnt = this.nameServerController.getRouteInfoManager().wipeWritePermOfBrokerByLock(requestHeader.getBrokerName());

        log.info("wipe write perm of broker[{}], client: {}, {}",
                requestHeader.getBrokerName(),
                RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
                wipeTopicCnt);

        responseHeader.setWipeTopicCount(wipeTopicCnt);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getAllTopicListFromNameserver(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] body = this.nameServerController.getRouteInfoManager().getAllTopicList();

        response.setBody(body);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand deleteTopicInNamesrv(ChannelHandlerContext ctx,
                                                 RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final DeleteTopicInNamesrvRequestHeader requestHeader =
                (DeleteTopicInNamesrvRequestHeader) request.decodeCommandCustomHeader(DeleteTopicInNamesrvRequestHeader.class);

        this.nameServerController.getRouteInfoManager().deleteTopic(requestHeader.getTopic());

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getKVListByNamespace(ChannelHandlerContext ctx,
                                                 RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final GetKVListByNamespaceRequestHeader requestHeader =
                (GetKVListByNamespaceRequestHeader) request.decodeCommandCustomHeader(GetKVListByNamespaceRequestHeader.class);

        byte[] jsonValue = this.nameServerController.getKvConfigManager().getKVListByNamespace(
                requestHeader.getNamespace());
        if (null != jsonValue) {
            response.setBody(jsonValue);
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);
            return response;
        }

        response.setCode(ResponseCode.QUERY_NOT_FOUND);
        response.setRemark("No config item, Namespace: " + requestHeader.getNamespace());
        return response;
    }

    private RemotingCommand getTopicsByCluster(ChannelHandlerContext ctx,
                                               RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final GetTopicsByClusterRequestHeader requestHeader =
                (GetTopicsByClusterRequestHeader) request.decodeCommandCustomHeader(GetTopicsByClusterRequestHeader.class);

        byte[] body = this.nameServerController.getRouteInfoManager().getTopicsByCluster(requestHeader.getCluster());

        response.setBody(body);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getSystemTopicListFromNs(ChannelHandlerContext ctx,
                                                     RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] body = this.nameServerController.getRouteInfoManager().getSystemTopicList();

        response.setBody(body);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getUnitTopicList(ChannelHandlerContext ctx,
                                             RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] body = this.nameServerController.getRouteInfoManager().getUnitTopics();

        response.setBody(body);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getHasUnitSubTopicList(ChannelHandlerContext ctx,
                                                   RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] body = this.nameServerController.getRouteInfoManager().getHasUnitSubTopicList();

        response.setBody(body);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getHasUnitSubUnUnitTopicList(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] body = this.nameServerController.getRouteInfoManager().getHasUnitSubUnUnitTopicList();

        response.setBody(body);
        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand updateConfig(ChannelHandlerContext ctx, RemotingCommand request) {
        log.info("updateConfig called by {}", RemotingHelper.parseChannelRemoteAddr(ctx.channel()));

        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        byte[] body = request.getBody();
        if (body != null) {
            String bodyStr;
            try {
                bodyStr = new String(body, MixAll.DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                log.error("updateConfig byte array to string error: ", e);
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("UnsupportedEncodingException " + e);
                return response;
            }

            Properties properties = MixAll.string2Properties(bodyStr);
            if (properties == null) {
                log.error("updateConfig MixAll.string2Properties error {}", bodyStr);
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("string2Properties error");
                return response;
            }

            this.nameServerController.getConfiguration().update(properties);
        }

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }

    private RemotingCommand getConfig(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);

        String content = this.nameServerController.getConfiguration().getAllConfigsFormatString();
        if (content != null && content.length() > 0) {
            try {
                response.setBody(content.getBytes(MixAll.DEFAULT_CHARSET));
            } catch (UnsupportedEncodingException e) {
                log.error("getConfig error, ", e);
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("UnsupportedEncodingException " + e);
                return response;
            }
        }

        response.setCode(ResponseCode.SUCCESS);
        response.setRemark(null);
        return response;
    }
}
