package cn.xianyijun.wisp.broker.plugin;

import cn.xianyijun.wisp.store.MessageStore;

import java.lang.reflect.Constructor;

/**
 * @author xianyijun
 */
public final class MessageStoreFactory {

    public static MessageStore build(MessageStorePluginContext context, MessageStore messageStore) {
        String plugin = context.getBrokerConfig().getMessageStorePlugIn();
        if (plugin != null && plugin.trim().length() != 0) {
            String[] pluginClasses = plugin.split(",");
            for (int i = pluginClasses.length - 1; i >= 0; --i) {
                String pluginClass = pluginClasses[i];
                try {
                    Class<AbstractPluginMessageStore> clazz = (Class<AbstractPluginMessageStore>) Class.forName(pluginClass);
                    Constructor<AbstractPluginMessageStore> construct = clazz.getConstructor(MessageStorePluginContext.class, MessageStore.class);
                    messageStore = construct.newInstance(context, messageStore);
                } catch (Throwable e) {
                    throw new RuntimeException(String.format(
                            "Initialize plugin's class %s not found!", pluginClass), e);
                }
            }
        }
        return messageStore;
    }

}
