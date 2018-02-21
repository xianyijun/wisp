package cn.xianyijun.wisp.broker.out;

import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.namesrv.RegisterBrokerResult;
import cn.xianyijun.wisp.common.namesrv.TopAddressing;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.body.ConsumerOffsetSerializeWrapper;
import cn.xianyijun.wisp.common.protocol.body.KVTable;
import cn.xianyijun.wisp.common.protocol.body.RegisterBrokerBody;
import cn.xianyijun.wisp.common.protocol.body.SubscriptionGroupWrapper;
import cn.xianyijun.wisp.common.protocol.body.TopicConfigSerializeWrapper;
import cn.xianyijun.wisp.common.protocol.header.namesrv.RegisterBrokerRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.namesrv.RegisterBrokerResponseHeader;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.RemotingCommandException;
import cn.xianyijun.wisp.exception.RemotingConnectException;
import cn.xianyijun.wisp.exception.RemotingSendRequestException;
import cn.xianyijun.wisp.exception.RemotingTimeoutException;
import cn.xianyijun.wisp.exception.RemotingTooMuchRequestException;
import cn.xianyijun.wisp.remoting.RPCHook;
import cn.xianyijun.wisp.remoting.RemotingClient;
import cn.xianyijun.wisp.remoting.netty.NettyClientConfig;
import cn.xianyijun.wisp.remoting.netty.NettyRemotingClient;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author xianyijun
 */
@Slf4j
@Getter
public class BrokerOuter {

    private final RemotingClient remotingClient;

    private final TopAddressing topAddressing = new TopAddressing(MixAll.getWSAddr());

    private String nameServerAddr = null;


    public BrokerOuter(final NettyClientConfig nettyClientConfig) {
        this(nettyClientConfig, null);
    }

    public BrokerOuter(final NettyClientConfig nettyClientConfig, RPCHook rpcHook) {
        this.remotingClient = new NettyRemotingClient(nettyClientConfig);
        this.remotingClient.registerRPCHook(rpcHook);
    }

    public void start() {
        this.remotingClient.start();
    }

    public void updateNameServerAddressList(final String addressListStr) {
        List<String> lst = new ArrayList<>();
        String[] addrArray = addressListStr.split(";");
        Collections.addAll(lst, addrArray);
        this.remotingClient.updateNameServerAddressList(lst);
    }


    public String fetchNameServerAddr() {
        try {
            String addrListStr = this.topAddressing.fetchNameServerAddr();
            if (addrListStr != null) {
                if (!addrListStr.equals(this.nameServerAddr)) {
                    log.info("name server address changed, old: {} new: {}", this.nameServerAddr, addrListStr);
                    this.updateNameServerAddressList(addrListStr);
                    this.nameServerAddr = addrListStr;
                    return nameServerAddr;
                }
            }
        } catch (Exception e) {
            log.error("fetchNameServerAddr Exception", e);
        }
        return nameServerAddr;
    }

    public TopicConfigSerializeWrapper getAllTopicConfig(
            final String addr) throws RemotingConnectException, RemotingSendRequestException,
            RemotingTimeoutException, InterruptedException, BrokerException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(true, addr), request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return TopicConfigSerializeWrapper.decode(response.getBody(), TopicConfigSerializeWrapper.class);
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public ConsumerOffsetSerializeWrapper getAllConsumerOffset(
            final String addr) throws InterruptedException, RemotingTimeoutException,
            RemotingSendRequestException, RemotingConnectException, BrokerException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_CONSUMER_OFFSET, null);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ConsumerOffsetSerializeWrapper.decode(response.getBody(), ConsumerOffsetSerializeWrapper.class);
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public String getAllDelayOffset(
            final String addr) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
            RemotingConnectException, BrokerException, UnsupportedEncodingException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_DELAY_OFFSET, null);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return new String(response.getBody(), MixAll.DEFAULT_CHARSET);
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public SubscriptionGroupWrapper getAllSubscriptionGroupConfig(
            final String addr) throws InterruptedException, RemotingTimeoutException,
            RemotingSendRequestException, RemotingConnectException, BrokerException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return SubscriptionGroupWrapper.decode(response.getBody(), SubscriptionGroupWrapper.class);
            }
            default:
                break;
        }
        throw new BrokerException(response.getCode(), response.getRemark());
    }

    public RegisterBrokerResult registerBrokerAll(
            final String clusterName,
            final String brokerAddr,
            final String brokerName,
            final long brokerId,
            final String haServerAddr,
            final TopicConfigSerializeWrapper topicConfigWrapper,
            final List<String> filterServerList,
            final boolean oneway,
            final int timeoutMills) {
        RegisterBrokerResult registerBrokerResult = null;

        List<String> nameServerAddressList = this.remotingClient.getNameServerAddressList();
        if (nameServerAddressList != null) {
            for (String nameServerAddr : nameServerAddressList) {
                try {
                    RegisterBrokerResult result = this.registerBroker(nameServerAddr, clusterName, brokerAddr, brokerName, brokerId,
                            haServerAddr, topicConfigWrapper, filterServerList, oneway, timeoutMills);
                    if (result != null) {
                        registerBrokerResult = result;
                    }

                    log.info("register broker to name server {} OK", nameServerAddr);
                } catch (Exception e) {
                    log.warn("registerBroker Exception, {}", nameServerAddr, e);
                }
            }
        }

        return registerBrokerResult;
    }

    private RegisterBrokerResult registerBroker(
            final String nameServerAddr,
            final String clusterName,
            final String brokerAddr,
            final String brokerName,
            final long brokerId,
            final String haServerAddr,
            final TopicConfigSerializeWrapper topicConfigWrapper,
            final List<String> filterServerList,
            final boolean oneWay,
            final int timeoutMills
    ) throws RemotingCommandException, BrokerException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
            InterruptedException {
        RegisterBrokerRequestHeader requestHeader = new RegisterBrokerRequestHeader();
        requestHeader.setBrokerAddr(brokerAddr);
        requestHeader.setBrokerId(brokerId);
        requestHeader.setBrokerName(brokerName);
        requestHeader.setClusterName(clusterName);
        requestHeader.setHaServerAddr(haServerAddr);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.REGISTER_BROKER, requestHeader);

        RegisterBrokerBody requestBody = new RegisterBrokerBody();
        requestBody.setTopicConfigSerializeWrapper(topicConfigWrapper);
        requestBody.setFilterServerList(filterServerList);
        request.setBody(requestBody.encode());

        if (oneWay) {
            try {
                this.remotingClient.invokeOneWay(nameServerAddr, request, timeoutMills);
            } catch (RemotingTooMuchRequestException ignored) {
            }
            return null;
        }

        RemotingCommand response = this.remotingClient.invokeSync(nameServerAddr, request, timeoutMills);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                RegisterBrokerResponseHeader responseHeader =
                        (RegisterBrokerResponseHeader) response.decodeCommandCustomHeader(RegisterBrokerResponseHeader.class);
                RegisterBrokerResult result = new RegisterBrokerResult();
                result.setMasterAddr(responseHeader.getMasterAddr());
                result.setHaServerAddr(responseHeader.getHaServerAddr());
                if (response.getBody() != null) {
                    result.setKvTable(KVTable.decode(response.getBody(), KVTable.class));
                }
                return result;
            }
            default:
                break;
        }

        throw new BrokerException(response.getCode(), response.getRemark());
    }

}
