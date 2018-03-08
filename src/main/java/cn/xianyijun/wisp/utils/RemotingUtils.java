package cn.xianyijun.wisp.utils;

import cn.xianyijun.wisp.common.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RemotingUtils {

    public static void closeChannel(Channel channel) {
        final String addrRemote = RemotingHelper.parseChannelRemoteAddr(channel);
        channel.close().addListener((ChannelFutureListener) future -> log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                future.isSuccess()));
    }

    public static String getLocalAddress() {
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> ipv4Result = new ArrayList<String>();
            ArrayList<String> ipv6Result = new ArrayList<String>();
            while (enumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = enumeration.nextElement();
                final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()) {
                    final InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet6Address) {
                            ipv6Result.add(normalizeHostAddress(address));
                        } else {
                            ipv4Result.add(normalizeHostAddress(address));
                        }
                    }
                }
            }

            if (!ipv4Result.isEmpty()) {
                for (String ip : ipv4Result) {
                    if (ip.startsWith("127.0") || ip.startsWith("192.168")) {
                        continue;
                    }

                    return ip;
                }

                return ipv4Result.get(ipv4Result.size() - 1);
            } else if (!ipv6Result.isEmpty()) {
                return ipv6Result.get(0);
            }
            //If failed to find,fall back to localhost
            final InetAddress localHost = InetAddress.getLocalHost();
            return normalizeHostAddress(localHost);
        } catch (Exception e) {
            log.error("Failed to obtain local address", e);
        }

        return null;
    }

    private static String normalizeHostAddress(final InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return "[" + localHost.getHostAddress() + "]";
        } else {
            return localHost.getHostAddress();
        }
    }

    public static SocketAddress string2SocketAddress(final String addr) {
        String[] s = addr.split(":");
        return new InetSocketAddress(s[0], Integer.parseInt(s[1]));
    }


    public static SocketChannel connect(SocketAddress remote) {
        return connect(remote, 1000 * 5);
    }

    public static SocketChannel connect(SocketAddress remote, final int timeoutMillis) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(true);
            sc.socket().setSoLinger(false, -1);
            sc.socket().setTcpNoDelay(true);
            sc.socket().setReceiveBufferSize(1024 * 64);
            sc.socket().setSendBufferSize(1024 * 64);
            sc.socket().connect(remote, timeoutMillis);
            sc.configureBlocking(false);
            return sc;
        } catch (Exception e) {
            if (sc != null) {
                try {
                    sc.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    public static Selector openSelector() throws IOException {
        Selector result = null;

        try {
            final Class<?> providerClazz = Class.forName("sun.nio.ch.EPollSelectorProvider");
            if (providerClazz != null) {
                try {
                    final Method method = providerClazz.getMethod("provider");
                    if (method != null) {
                        final SelectorProvider selectorProvider = (SelectorProvider) method.invoke(null);
                        if (selectorProvider != null) {
                            result = selectorProvider.openSelector();
                        }
                    }
                } catch (final Exception e) {
                    log.warn("Open ePoll Selector for linux platform exception", e);
                }
            }
        } catch (final Exception e) {
            // ignore
        }

        if (result == null) {
            result = Selector.open();
        }

        return result;
    }

    public static String socketAddress2String(final SocketAddress addr) {
        return ((InetSocketAddress)addr).getAddress().getHostAddress() +
                ":" +
                ((InetSocketAddress)addr).getPort();
    }
}
