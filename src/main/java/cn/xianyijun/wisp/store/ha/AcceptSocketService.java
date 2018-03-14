package cn.xianyijun.wisp.store.ha;

import cn.xianyijun.wisp.common.ServiceThread;
import cn.xianyijun.wisp.utils.RemotingUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * @author xianyijun
 */
@Slf4j
public class AcceptSocketService extends ServiceThread {
    private final HAService haService;
    private final SocketAddress socketAddressListen;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    AcceptSocketService(HAService haService, final int port) {
        this.haService = haService;
        this.socketAddressListen = new InetSocketAddress(port);
    }

    /**
     * Starts listening to slave connections.
     *
     * @throws Exception If fails.
     */
    public void beginAccept() throws Exception {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.selector = RemotingUtils.openSelector();
        this.serverSocketChannel.socket().setReuseAddress(true);
        this.serverSocketChannel.socket().bind(this.socketAddressListen);
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown(final boolean interrupt) {
        super.shutdown(interrupt);
        try {
            this.serverSocketChannel.close();
            this.selector.close();
        } catch (IOException e) {
            log.error("AcceptSocketService shutdown exception", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.selector.select(1000);
                Set<SelectionKey> selected = this.selector.selectedKeys();

                if (selected != null) {
                    for (SelectionKey k : selected) {
                        if ((k.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
                            SocketChannel sc = ((ServerSocketChannel) k.channel()).accept();

                            if (sc != null) {
                                log.info("HAService receive new connection, "
                                        + sc.socket().getRemoteSocketAddress());

                                try {
                                    HAConnection conn = new HAConnection(this.haService, sc);
                                    conn.start();
                                    haService.addConnection(conn);
                                } catch (Exception e) {
                                    log.error("new HAConnection exception", e);
                                    sc.close();
                                }
                            }
                        } else {
                            log.warn("Unexpected ops in select " + k.readyOps());
                        }
                    }

                    selected.clear();
                }
            } catch (Exception e) {
                log.error(this.getServiceName() + " service has exception.", e);
            }
        }

        log.info(this.getServiceName() + " service end");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceName() {
        return AcceptSocketService.class.getSimpleName();
    }
}

