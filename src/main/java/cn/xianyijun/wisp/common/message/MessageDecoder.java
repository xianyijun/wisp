package cn.xianyijun.wisp.common.message;

import cn.xianyijun.wisp.common.UtilAll;
import cn.xianyijun.wisp.common.sysflag.MessageSysFlag;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author xianyijun
 */
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

        byte[] data = UtilAll.string2bytes(msgId.substring(16, 32));
        bb = ByteBuffer.wrap(Objects.requireNonNull(data));
        offset = bb.getLong(0);

        return new MessageId(address, offset);
    }

    public static String createMessageId(final ByteBuffer input, final ByteBuffer addr, final long offset) {
        input.flip();
        input.limit(MessageDecoder.MSG_ID_LENGTH);

        input.put(addr);
        input.putLong(offset);

        return UtilAll.bytes2string(input.array());
    }

    public static Map<String, String> decodeProperties(java.nio.ByteBuffer byteBuffer) {
        int topicLengthPosition = BODY_SIZE_POSITION + 4 + byteBuffer.getInt(BODY_SIZE_POSITION);

        byte topicLength = byteBuffer.get(topicLengthPosition);

        short propertiesLength = byteBuffer.getShort(topicLengthPosition + 1 + topicLength);

        byteBuffer.position(topicLengthPosition + 1 + topicLength + 2);

        if (propertiesLength > 0) {
            byte[] properties = new byte[propertiesLength];
            byteBuffer.get(properties);
            String propertiesString = new String(properties, CHARSET_UTF8);
            Map<String, String> map = string2messageProperties(propertiesString);
            return map;
        }
        return null;
    }

    public static ExtMessage decode(java.nio.ByteBuffer byteBuffer) {
        return decode(byteBuffer, true, true, false);
    }

    public static ExtMessage clientDecode(java.nio.ByteBuffer byteBuffer, final boolean readBody) {
        return decode(byteBuffer, readBody, true, true);
    }

    public static ExtMessage decode(java.nio.ByteBuffer byteBuffer, final boolean readBody) {
        return decode(byteBuffer, readBody, true, false);
    }

    public static ExtMessage decode(
            java.nio.ByteBuffer byteBuffer, final boolean readBody, final boolean deCompressBody) {
        return decode(byteBuffer, readBody, deCompressBody, false);
    }

    public static byte[] encode(ExtMessage messageExt, boolean needCompress) throws Exception {
        byte[] body = messageExt.getBody();
        byte[] topics = messageExt.getTopic().getBytes(CHARSET_UTF8);
        byte topicLen = (byte) topics.length;
        String properties = messageProperties2String(messageExt.getProperties());
        byte[] propertiesBytes = properties.getBytes(CHARSET_UTF8);
        short propertiesLength = (short) propertiesBytes.length;
        int sysFlag = messageExt.getSysFlag();
        byte[] newBody = messageExt.getBody();
        if (needCompress && (sysFlag & MessageSysFlag.COMPRESSED_FLAG) == MessageSysFlag.COMPRESSED_FLAG) {
            newBody = UtilAll.compress(body, 5);
        }
        int bodyLength = newBody.length;
        int storeSize = messageExt.getStoreSize();
        ByteBuffer byteBuffer;
        if (storeSize > 0) {
            byteBuffer = ByteBuffer.allocate(storeSize);
        } else {
            storeSize = 4 // 1 TOTALSIZE
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
                    + 8 // 14 Prepared Transaction Offset
                    + 4 + bodyLength // 14 BODY
                    + 1 + topicLen // 15 TOPIC
                    + 2 + propertiesLength;
            byteBuffer = ByteBuffer.allocate(storeSize);
        }
        // 1 TOTALSIZE
        byteBuffer.putInt(storeSize);

        // 2 MAGICCODE
        byteBuffer.putInt(MESSAGE_MAGIC_CODE);

        // 3 BODYCRC
        int bodyCRC = messageExt.getBodyCRC();
        byteBuffer.putInt(bodyCRC);

        // 4 QUEUEID
        int queueId = messageExt.getQueueId();
        byteBuffer.putInt(queueId);

        // 5 FLAG
        int flag = messageExt.getFlag();
        byteBuffer.putInt(flag);

        // 6 QUEUEOFFSET
        long queueOffset = messageExt.getQueueOffset();
        byteBuffer.putLong(queueOffset);

        // 7 PHYSICALOFFSET
        long physicOffset = messageExt.getCommitLogOffset();
        byteBuffer.putLong(physicOffset);

        // 8 SYSFLAG
        byteBuffer.putInt(sysFlag);

        // 9 BORNTIMESTAMP
        long bornTimeStamp = messageExt.getBornTimestamp();
        byteBuffer.putLong(bornTimeStamp);

        // 10 BORNHOST
        InetSocketAddress bornHost = (InetSocketAddress) messageExt.getBornHost();
        byteBuffer.put(bornHost.getAddress().getAddress());
        byteBuffer.putInt(bornHost.getPort());

        // 11 STORETIMESTAMP
        long storeTimestamp = messageExt.getStoreTimestamp();
        byteBuffer.putLong(storeTimestamp);

        // 12 STOREHOST
        InetSocketAddress serverHost = (InetSocketAddress) messageExt.getStoreHost();
        byteBuffer.put(serverHost.getAddress().getAddress());
        byteBuffer.putInt(serverHost.getPort());

        // 13 RECONSUMETIMES
        int reconsumeTimes = messageExt.getReConsumeTimes();
        byteBuffer.putInt(reconsumeTimes);

        // 14 Prepared Transaction Offset
        long preparedTransactionOffset = messageExt.getPreparedTransactionOffset();
        byteBuffer.putLong(preparedTransactionOffset);

        // 15 BODY
        byteBuffer.putInt(bodyLength);
        byteBuffer.put(newBody);

        // 16 TOPIC
        byteBuffer.put(topicLen);
        byteBuffer.put(topics);

        // 17 properties
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);

        return byteBuffer.array();
    }

    public static ExtMessage decode(
            java.nio.ByteBuffer byteBuffer, final boolean readBody, final boolean deCompressBody, final boolean isClient) {
        try {

            ExtMessage msgExt;
            if (isClient) {
                msgExt = new ExtClientMessage();
            } else {
                msgExt = new ExtMessage();
            }

            int storeSize = byteBuffer.getInt();
            msgExt.setStoreSize(storeSize);

            byteBuffer.getInt();

            int bodyCRC = byteBuffer.getInt();
            msgExt.setBodyCRC(bodyCRC);

            int queueId = byteBuffer.getInt();
            msgExt.setQueueId(queueId);

            int flag = byteBuffer.getInt();
            msgExt.setFlag(flag);

            long queueOffset = byteBuffer.getLong();
            msgExt.setQueueOffset(queueOffset);

            long physicOffset = byteBuffer.getLong();
            msgExt.setCommitLogOffset(physicOffset);

            int sysFlag = byteBuffer.getInt();
            msgExt.setSysFlag(sysFlag);

            long bornTimeStamp = byteBuffer.getLong();
            msgExt.setBornTimestamp(bornTimeStamp);

            byte[] bornHost = new byte[4];
            byteBuffer.get(bornHost, 0, 4);
            int port = byteBuffer.getInt();
            msgExt.setBornHost(new InetSocketAddress(InetAddress.getByAddress(bornHost), port));

            long storeTimestamp = byteBuffer.getLong();
            msgExt.setStoreTimestamp(storeTimestamp);

            byte[] storeHost = new byte[4];
            byteBuffer.get(storeHost, 0, 4);
            port = byteBuffer.getInt();
            msgExt.setStoreHost(new InetSocketAddress(InetAddress.getByAddress(storeHost), port));

            int reConsumeTimes = byteBuffer.getInt();
            msgExt.setReConsumeTimes(reConsumeTimes);

            long preparedTransactionOffset = byteBuffer.getLong();
            msgExt.setPreparedTransactionOffset(preparedTransactionOffset);

            int bodyLen = byteBuffer.getInt();
            if (bodyLen > 0) {
                if (readBody) {
                    byte[] body = new byte[bodyLen];
                    byteBuffer.get(body);

                    // uncompress body
                    if (deCompressBody && (sysFlag & MessageSysFlag.COMPRESSED_FLAG) == MessageSysFlag.COMPRESSED_FLAG) {
                        body = UtilAll.unCompress(body);
                    }

                    msgExt.setBody(body);
                } else {
                    byteBuffer.position(byteBuffer.position() + bodyLen);
                }
            }

            // 16 TOPIC
            byte topicLen = byteBuffer.get();
            byte[] topic = new byte[(int) topicLen];
            byteBuffer.get(topic);
            msgExt.setTopic(new String(topic, CHARSET_UTF8));

            // 17 properties
            short propertiesLength = byteBuffer.getShort();
            if (propertiesLength > 0) {
                byte[] properties = new byte[propertiesLength];
                byteBuffer.get(properties);
                String propertiesString = new String(properties, CHARSET_UTF8);
                Map<String, String> map = string2messageProperties(propertiesString);
                msgExt.setProperties(map);
            }

            ByteBuffer byteBufferMsgId = ByteBuffer.allocate(MSG_ID_LENGTH);
            String msgId = createMessageId(byteBufferMsgId, msgExt.getStoreHostBytes(), msgExt.getCommitLogOffset());
            msgExt.setMsgId(msgId);

            if (isClient) {
                ((ExtClientMessage) msgExt).setOffsetMsgId(msgId);
            }

            return msgExt;
        } catch (Exception e) {
            byteBuffer.position(byteBuffer.limit());
        }

        return null;
    }

    public static List<ExtMessage> decodes(java.nio.ByteBuffer byteBuffer) {
        return decodes(byteBuffer, true);
    }

    public static List<ExtMessage> decodes(java.nio.ByteBuffer byteBuffer, final boolean readBody) {
        List<ExtMessage> extMessages = new ArrayList<>();
        while (byteBuffer.hasRemaining()) {
            ExtMessage msgExt = clientDecode(byteBuffer, readBody);
            if (null != msgExt) {
                extMessages.add(msgExt);
            } else {
                break;
            }
        }
        return extMessages;
    }

    public static byte[] encodeMessages(List<Message> messages) {
        List<byte[]> encodedMessages = new ArrayList<>(messages.size());
        int allSize = 0;
        for (Message message : messages) {
            byte[] tmp = encodeMessage(message);
            encodedMessages.add(tmp);
            allSize += tmp.length;
        }
        byte[] allBytes = new byte[allSize];
        int pos = 0;
        for (byte[] bytes : encodedMessages) {
            System.arraycopy(bytes, 0, allBytes, pos, bytes.length);
            pos += bytes.length;
        }
        return allBytes;
    }

    public static byte[] encodeMessage(Message message) {
        byte[] body = message.getBody();
        int bodyLen = body.length;
        String properties = messageProperties2String(message.getProperties());
        byte[] propertiesBytes = properties.getBytes(CHARSET_UTF8);
        short propertiesLength = (short) propertiesBytes.length;
        int storeSize = 4 // 1 TOTALSIZE
                + 4 // 2 MAGICCOD
                + 4 // 3 BODYCRC
                + 4 // 4 FLAG
                + 4 + bodyLen // 4 BODY
                + 2 + propertiesLength;
        ByteBuffer byteBuffer = ByteBuffer.allocate(storeSize);
        byteBuffer.putInt(storeSize);

        byteBuffer.putInt(0);

        byteBuffer.putInt(0);

        int flag = message.getFlag();
        byteBuffer.putInt(flag);

        byteBuffer.putInt(bodyLen);
        byteBuffer.put(body);

        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);

        return byteBuffer.array();
    }


}
