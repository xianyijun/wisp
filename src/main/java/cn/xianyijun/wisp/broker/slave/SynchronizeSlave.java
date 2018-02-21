package cn.xianyijun.wisp.broker.slave;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.subscription.SubscriptionGroupManager;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.protocol.body.ConsumerOffsetSerializeWrapper;
import cn.xianyijun.wisp.common.protocol.body.SubscriptionGroupWrapper;
import cn.xianyijun.wisp.common.protocol.body.TopicConfigSerializeWrapper;
import cn.xianyijun.wisp.store.config.StorePathConfigHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author xianyijun
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class SynchronizeSlave {

    private final BrokerController brokerController;

    @Setter
    private volatile String masterAddr = null;

    public void syncAll() {
        this.syncTopicConfig();
        this.syncConsumerOffset();
        this.syncDelayOffset();
        this.syncSubscriptionGroupConfig();
    }

    private void syncTopicConfig() {
        String masterAddrBak = this.masterAddr;
        if (masterAddrBak != null) {
            try {
                TopicConfigSerializeWrapper topicWrapper =
                        this.brokerController.getBrokerOuter().getAllTopicConfig(masterAddrBak);
                if (!this.brokerController.getTopicConfigManager().getDataVersion()
                        .equals(topicWrapper.getDataVersion())) {

                    this.brokerController.getTopicConfigManager().getDataVersion()
                            .assignNewOne(topicWrapper.getDataVersion());
                    this.brokerController.getTopicConfigManager().getTopicConfigTable().clear();
                    this.brokerController.getTopicConfigManager().getTopicConfigTable()
                            .putAll(topicWrapper.getTopicConfigTable());
                    this.brokerController.getTopicConfigManager().persist();

                    log.info("Update slave topic config from master, {}", masterAddrBak);
                }
            } catch (Exception e) {
                log.error("SyncTopicConfig Exception, {}", masterAddrBak, e);
            }
        }
    }

    private void syncConsumerOffset() {
        String masterAddrBak = this.masterAddr;
        if (masterAddrBak != null) {
            try {
                ConsumerOffsetSerializeWrapper offsetWrapper =
                        this.brokerController.getBrokerOuter().getAllConsumerOffset(masterAddrBak);
                this.brokerController.getConsumerOffsetManager().getOffsetTable()
                        .putAll(offsetWrapper.getOffsetTable());
                this.brokerController.getConsumerOffsetManager().persist();
                log.info("Update slave consumer offset from master, {}", masterAddrBak);
            } catch (Exception e) {
                log.error("SyncConsumerOffset Exception, {}", masterAddrBak, e);
            }
        }
    }

    private void syncDelayOffset() {
        String masterAddrBak = this.masterAddr;
        if (masterAddrBak != null) {
            try {
                String delayOffset =
                        this.brokerController.getBrokerOuter().getAllDelayOffset(masterAddrBak);
                if (delayOffset != null) {

                    String fileName =
                            StorePathConfigHelper.getDelayOffsetStorePath(this.brokerController
                                    .getMessageStoreConfig().getStorePathRootDir());
                    try {
                        MixAll.string2File(delayOffset, fileName);
                    } catch (IOException e) {
                        log.error("Persist file Exception, {}", fileName, e);
                    }
                }
                log.info("Update slave delay offset from master, {}", masterAddrBak);
            } catch (Exception e) {
                log.error("SyncDelayOffset Exception, {}", masterAddrBak, e);
            }
        }
    }

    private void syncSubscriptionGroupConfig() {
        String masterAddrBak = this.masterAddr;
        if (masterAddrBak != null) {
            try {
                SubscriptionGroupWrapper subscriptionWrapper =
                        this.brokerController.getBrokerOuter()
                                .getAllSubscriptionGroupConfig(masterAddrBak);

                if (!this.brokerController.getSubscriptionGroupManager().getDataVersion()
                        .equals(subscriptionWrapper.getDataVersion())) {
                    SubscriptionGroupManager subscriptionGroupManager =
                            this.brokerController.getSubscriptionGroupManager();
                    subscriptionGroupManager.getDataVersion().assignNewOne(
                            subscriptionWrapper.getDataVersion());
                    subscriptionGroupManager.getSubscriptionGroupTable().clear();
                    subscriptionGroupManager.getSubscriptionGroupTable().putAll(
                            subscriptionWrapper.getSubscriptionGroupTable());
                    subscriptionGroupManager.persist();
                    log.info("Update slave Subscription Group from master, {}", masterAddrBak);
                }
            } catch (Exception e) {
                log.error("SyncSubscriptionGroup Exception, {}", masterAddrBak, e);
            }
        }
    }
}
