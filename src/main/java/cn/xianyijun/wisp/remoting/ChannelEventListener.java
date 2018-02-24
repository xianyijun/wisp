package cn.xianyijun.wisp.remoting;

import io.netty.channel.Channel;

/**
 * The interface Channel event listener.
 * @author xianyijun
 */
public interface ChannelEventListener {
    /**
     * On channel connect.
     *
     * @param remoteAddr the remote addr
     * @param channel    the channel
     */
    void onChannelConnect(final String remoteAddr, final Channel channel);

    /**
     * On channel close.
     *
     * @param remoteAddr the remote addr
     * @param channel    the channel
     */
    void onChannelClose(final String remoteAddr, final Channel channel);

    /**
     * On channel exception.
     *
     * @param remoteAddr the remote addr
     * @param channel    the channel
     */
    void onChannelException(final String remoteAddr, final Channel channel);

    /**
     * On channel idle.
     *
     * @param remoteAddr the remote addr
     * @param channel    the channel
     */
    void onChannelIdle(final String remoteAddr, final Channel channel);
}
