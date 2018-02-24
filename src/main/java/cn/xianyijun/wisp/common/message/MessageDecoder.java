package cn.xianyijun.wisp.common.message;

import cn.xianyijun.wisp.common.UtilAll;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessageDecoder {
    public final static int MSG_ID_LENGTH = 8 + 8;

    public final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    public final static int MESSAGE_MAGIC_CODE_POSITION = 4;
    public final static int MESSAGE_STORE_TIMESTAMP_POSITION = 56;
    public final static int MESSAGE_MAGIC_CODE = 0xAABBCCDD ^ 1880681586 + 8;
    public static final char NAME_VALUE_SEPARATOR = 1;
    public static final char PROPERTY_SEPARATOR = 2;
    public static final int BODY_SIZE_POSITION = 4 // 1 TOTALSIZE
            + 4 // 2 MAGICCODE
            + 4 // 3 BODYCRC
            + 4 // 4 QUEUEID
            + 4 // 5 FLAG
            + 8 // 6 QUEUEOFFSET
            + 8 // 7 PHYSICALOFFSET
            + 4 // 8 SYSFLAG
            + 8 // 9 BORNTIMESTAMP
            + 8 // 10 BORNHOST
            + 8 // 11 STORETIMESTAMP
            + 8 // 12 STOREHOSTADDRESS
            + 4 // 13 RECONSUMETIMES
            + 8; // 14 Prepared Transaction Offset

    public static Map<String, String> string2messageProperties(final String properties) {
        Map<String, String> map = new HashMap<>();
        if (properties != null) {
            String[] items = properties.split(String.valueOf(PROPERTY_SEPARATOR));
            for (String i : items) {
                String[] nv = i.split(String.valueOf(NAME_VALUE_SEPARATOR));
                if (2 == nv.length) {
                    map.put(nv[0], nv[1]);
                }
            }
        }
        return map;
    }


    public static String messageProperties2String(Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                sb.append(name);
                sb.append(NAME_VALUE_SEPARATOR);
                sb.append(value);
                sb.append(PROPERTY_SEPARATOR);
            }
        }
        return sb.toString();
    }


    public static MessageId decodeMessageId(final String msgId) throws UnknownHostException {
        SocketAddress address;
        long offset;

        byte[] ip = UtilAll.string2bytes(msgId.substring(0, 8));
        byte[] port = UtilAll.string2bytes(msgId.substring(8, 16));
        ByteBuffer bb = ByteBuffer.wrap(Objects.requireNonNull(port));
        int portInt = bb.getInt(0);
        address = new InetSocketAddress(InetAddress.getByAddress(ip), portInt);

        // offset
        byte[] data = UtilAll.string2bytes(msgId.substring(16, 32));
        bb = ByteBuffer.wrap(data);
        offset = bb.getLong(0);

        return new MessageId(address, offset);
    }

}
