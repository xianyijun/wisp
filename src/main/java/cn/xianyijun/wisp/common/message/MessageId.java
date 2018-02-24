package cn.xianyijun.wisp.common.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.SocketAddress;

/**
 * @author xianyijun
 */
@Data
@AllArgsConstructor
public class MessageId {
    private SocketAddress address;
    private long offset;
}
