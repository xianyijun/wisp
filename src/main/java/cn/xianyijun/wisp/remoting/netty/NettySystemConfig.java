package cn.xianyijun.wisp.remoting.netty;

public class NettySystemConfig {
    public static final String WISP_REMOTING_SOCKET_SNDBUF_SIZE =
            "wisp.remoting.socket.sndbuf.size";
    public static final String WISP_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE =
            "wisp.remoting.socket.rcvbuf.size";

    public static int socketSndBufSize =
            Integer.parseInt(System.getProperty(WISP_REMOTING_SOCKET_SNDBUF_SIZE, "65535"));
    public static int socketRcvBufSize =
            Integer.parseInt(System.getProperty(WISP_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE, "65535"));
}
