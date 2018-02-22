package cn.xianyijun.wisp.remoting.netty;

public class NettySystemConfig {
    public static final String WISP_REMOTING_SOCKET_SNDBUF_SIZE =
            "wisp.remoting.socket.sndbuf.size";
    public static final String WISP_REMOTING_SOCKET_RCVBUF_SIZE =
            "wisp.remoting.socket.rcvbuf.size";

    public static final String WISP_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE = "wisp.remoting.client.async.semaphore.value";

    public static final String WISP_REMOTING_CLIENT_ONE_WAY_SEMAPHORE_VALUE = "wisp.remoting.client.one.way.semaphore.value";
    public static final int CLIENT_ASYNC_SEMAPHORE_VALUE =
            Integer.parseInt(System.getProperty(WISP_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE, "65535"));
    public static final int CLIENT_ONE_WAY_SEMAPHORE_VALUE =
            Integer.parseInt(System.getProperty(WISP_REMOTING_CLIENT_ONE_WAY_SEMAPHORE_VALUE, "65535"));
    public static int socketSndBufSize =
            Integer.parseInt(System.getProperty(WISP_REMOTING_SOCKET_SNDBUF_SIZE, "65535"));
    public static int socketRcvBufSize =
            Integer.parseInt(System.getProperty(WISP_REMOTING_SOCKET_RCVBUF_SIZE, "65535"));

}
