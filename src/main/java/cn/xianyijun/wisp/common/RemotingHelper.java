package cn.xianyijun.wisp.common;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.SocketAddress;

/**
 * The type Remoting helper.
 * @author xianyijun
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RemotingHelper {

    /**
     * Parse socket address addr string.
     *
     * @param socketAddress the socket address
     * @return the string
     */
    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();

            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }

    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }
        return "";
    }

    public static String exceptionSimpleDesc(final Throwable e) {
        StringBuffer sb = new StringBuffer();
        if (e != null) {
            sb.append(e.toString());

            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
                sb.append(", ");
                sb.append(element.toString());
            }
        }

        return sb.toString();
    }
}
