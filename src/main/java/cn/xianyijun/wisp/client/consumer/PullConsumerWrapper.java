package cn.xianyijun.wisp.client.consumer;

import cn.xianyijun.wisp.client.CommunicationMode;
import cn.xianyijun.wisp.client.hook.FilterMessageContext;
import cn.xianyijun.wisp.client.hook.FilterMessageHook;
import cn.xianyijun.wisp.client.producer.factory.ClientInstance;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.message.ExtMessage;
import cn.xianyijun.wisp.common.message.MessageAccessor;
import cn.xianyijun.wisp.common.message.MessageConst;
import cn.xianyijun.wisp.common.message.MessageDecoder;
import cn.xianyijun.wisp.common.message.MessageQueue;
import cn.xianyijun.wisp.common.protocol.header.PullMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.heartbeat.SubscriptionData;
import cn.xianyijun.wisp.common.protocol.route.TopicRouteData;
import cn.xianyijun.wisp.common.sysflag.PullSysFlag;
import cn.xianyijun.wisp.exception.BrokerException;
import cn.xianyijun.wisp.exception.ClientException;
import cn.xianyijun.wisp.exception.RemotingException;
import cn.xianyijun.wisp.filter.ExpressionType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Getter
@Slf4j
public class PullConsumerWrapper {
    private final ClientInstance clientFactory;
    private final String consumerGroup;
    private final boolean unitMode;
    private ConcurrentMap<MessageQueue, AtomicLong/* brokerId */> pullFromWhichNodeTable =
            new ConcurrentHashMap<>(32);
    private volatile boolean connectBrokerByUser = false;
    private volatile long defaultBrokerId = MixAll.MASTER_ID;
    private Random random = new Random(System.currentTimeMillis());
    @Setter
    private ArrayList<FilterMessageHook> filterMessageHookList = new ArrayList<>();

    public PullConsumerWrapper(ClientInstance clientFactory, String consumerGroup, boolean unitMode) {
        this.clientFactory = clientFactory;
        this.consumerGroup = consumerGroup;
        this.unitMode = unitMode;
    }

    public PullResult processPullResult(final MessageQueue mq, final PullResult pullResult,
                                        final SubscriptionData subscriptionData) {
        ExtPullResult extPullResult = (ExtPullResult) pullResult;

        this.updatePullFromWhichNode(mq, extPullResult.getSuggestWhichBrokerId());
        if (PullStatus.FOUND == pullResult.getPullStatus()) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(extPullResult.getMessageBinary());
            List<ExtMessage> msgList = MessageDecoder.decodes(byteBuffer);

            List<ExtMessage> msgListFilterAgain = msgList;
            if (!subscriptionData.getTagsSet().isEmpty() && !subscriptionData.isClassFilterMode()) {
                msgListFilterAgain = new ArrayList<>(msgList.size());
                for (ExtMessage msg : msgList) {
                    if (msg.getTags() != null) {
                        if (subscriptionData.getTagsSet().contains(msg.getTags())) {
                            msgListFilterAgain.add(msg);
                        }
                    }
                }
            }

            if (this.hasHook()) {
                FilterMessageContext filterMessageContext = new FilterMessageContext();
                filterMessageContext.setUnitMode(unitMode);
                filterMessageContext.setMsgList(msgListFilterAgain);
                this.executeHook(filterMessageContext);
            }

            for (ExtMessage msg : msgListFilterAgain) {
                MessageAccessor.putProperty(msg, MessageConst.PROPERTY_MIN_OFFSET,
                        Long.toString(pullResult.getMinOffset()));
                MessageAccessor.putProperty(msg, MessageConst.PROPERTY_MAX_OFFSET,
                        Long.toString(pullResult.getMaxOffset()));
            }

            extPullResult.setMsgFoundList(msgListFilterAgain);
        }

        extPullResult.setMessageBinary(null);

        return pullResult;
    }


    public void updatePullFromWhichNode(final MessageQueue mq, final long brokerId) {
        AtomicLong suggest = this.pullFromWhichNodeTable.get(mq);
        if (null == suggest) {
            this.pullFromWhichNodeTable.put(mq, new AtomicLong(brokerId));
        } else {
            suggest.set(brokerId);
        }
    }

    private boolean hasHook() {
        return !this.filterMessageHookList.isEmpty();
    }

    private void executeHook(final FilterMessageContext context) {
        if (!this.filterMessageHookList.isEmpty()) {
            for (FilterMessageHook hook : this.filterMessageHookList) {
                try {
                    hook.filterMessage(context);
                } catch (Throwable e) {
                    log.error("execute hook error. hookName={}", hook.hookName());
                }
            }
        }
    }

    public PullResult doPullKernel(
            final MessageQueue mq,
            final String subExpression,
            final String expressionType,
            final long subVersion,
            final long offset,
            final int maxNums,
            final int sysFlag,
            final long commitOffset,
            final long brokerSuspendMaxTimeMillis,
            final long timeoutMillis,
            final CommunicationMode communicationMode,
            final PullCallback pullCallback
    ) throws ClientException, RemotingException, BrokerException, InterruptedException {
        FindBrokerResult findBrokerResult =
                this.clientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(),
                        this.recalculatePullFromWhichNode(mq), false);
        if (null == findBrokerResult) {
            this.clientFactory.updateTopicRouteInfoFromNameServer(mq.getTopic());
            findBrokerResult =
                    this.clientFactory.findBrokerAddressInSubscribe(mq.getBrokerName(),
                            this.recalculatePullFromWhichNode(mq), false);
        }

        if (findBrokerResult != null) {

            int sysFlagInner = sysFlag;

            if (findBrokerResult.isSlave()) {
                sysFlagInner = PullSysFlag.clearCommitOffsetFlag(sysFlagInner);
            }

            PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
            requestHeader.setConsumerGroup(this.consumerGroup);
            requestHeader.setTopic(mq.getTopic());
            requestHeader.setQueueId(mq.getQueueId());
            requestHeader.setQueueOffset(offset);
            requestHeader.setMaxMsgNums(maxNums);
            requestHeader.setSysFlag(sysFlagInner);
            requestHeader.setCommitOffset(commitOffset);
            requestHeader.setSuspendTimeoutMillis(brokerSuspendMaxTimeMillis);
            requestHeader.setSubscription(subExpression);
            requestHeader.setSubVersion(subVersion);
            requestHeader.setExpressionType(expressionType);

            String brokerAddr = findBrokerResult.getBrokerAddr();
            if (PullSysFlag.hasClassFilterFlag(sysFlagInner)) {
                brokerAddr = computePullFromWhichFilterServer(mq.getTopic(), brokerAddr);
            }

            return this.clientFactory.getClient().pullMessage(
                    brokerAddr,
                    requestHeader,
                    timeoutMillis,
                    communicationMode,
                    pullCallback);
        }

        throw new ClientException("The broker[" + mq.getBrokerName() + "] not exist", null);
    }

    public PullResult doPullKernel(
            final MessageQueue mq,
            final String subExpression,
            final long subVersion,
            final long offset,
            final int maxNums,
            final int sysFlag,
            final long commitOffset,
            final long brokerSuspendMaxTimeMillis,
            final long timeoutMillis,
            final CommunicationMode communicationMode,
            final PullCallback pullCallback
    ) throws ClientException, RemotingException, BrokerException, InterruptedException {
        return doPullKernel(mq, subExpression, ExpressionType.TAG, subVersion, offset, maxNums, sysFlag, commitOffset, brokerSuspendMaxTimeMillis, timeoutMillis, communicationMode, pullCallback
        );
    }


    private long recalculatePullFromWhichNode(final MessageQueue mq) {
        if (this.isConnectBrokerByUser()) {
            return this.defaultBrokerId;
        }

        AtomicLong suggest = this.pullFromWhichNodeTable.get(mq);
        if (suggest != null) {
            return suggest.get();
        }

        return MixAll.MASTER_ID;
    }

    private String computePullFromWhichFilterServer(final String topic, final String brokerAddr)
            throws ClientException {
        ConcurrentMap<String, TopicRouteData> topicRouteTable = this.clientFactory.getTopicRouteTable();
        if (topicRouteTable != null) {
            TopicRouteData topicRouteData = topicRouteTable.get(topic);
            List<String> list = topicRouteData.getFilterServerTable().get(brokerAddr);

            if (list != null && !list.isEmpty()) {
                return list.get(randomNum() % list.size());
            }
        }

        throw new ClientException("Find Filter Server Failed, Broker Addr: " + brokerAddr + " topic: "
                + topic, null);
    }

    public int randomNum() {
        int value = random.nextInt();
        if (value < 0) {
            value = Math.abs(value);
            if (value < 0) {
                value = 0;
            }
        }
        return value;
    }


}
