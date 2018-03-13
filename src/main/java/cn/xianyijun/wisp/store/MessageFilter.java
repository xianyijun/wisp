package cn.xianyijun.wisp.store;

import cn.xianyijun.wisp.store.io.ExtConsumeQueue;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * The interface Message filter.
 *
 * @author xianyijun
 */
public interface MessageFilter {
    /**
     * Is matched by consume queue boolean.
     *
     * @param tagsCode  the tags code
     * @param cqExtUnit the cq ext unit
     * @return the boolean
     */
    boolean isMatchedByConsumeQueue(final Long tagsCode,
                                    final ExtConsumeQueue.CqExtUnit cqExtUnit);

    /**
     * Is matched by commit log boolean.
     *
     * @param msgBuffer  the msg buffer
     * @param properties the properties
     * @return the boolean
     */
    boolean isMatchedByCommitLog(final ByteBuffer msgBuffer,
                                 final Map<String, String> properties);

}
