package cn.xianyijun.wisp.common.message;

import cn.xianyijun.wisp.common.UtilAll;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xianyijun
 */
public class MessageClientIDSetter {
    private static final int LEN;
    private static final String FIX_STRING;
    private static final AtomicInteger COUNTER;
    private static long startTime;
    private static long nextStartTime;

    static {
        LEN = 4 + 2 + 4 + 4 + 2;
        ByteBuffer tempBuffer = ByteBuffer.allocate(10);
        tempBuffer.position(2);
        tempBuffer.putInt(UtilAll.getPid());
        tempBuffer.position(0);
        try {
            tempBuffer.put(UtilAll.getIP());
        } catch (Exception e) {
            tempBuffer.put(createFakeIP());
        }
        tempBuffer.position(6);
        tempBuffer.putInt(MessageClientIDSetter.class.getClassLoader().hashCode());
        FIX_STRING = UtilAll.bytes2string(tempBuffer.array());
        setStartTime(System.currentTimeMillis());
        COUNTER = new AtomicInteger(0);
    }

    private synchronized static void setStartTime(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startTime = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        nextStartTime = cal.getTimeInMillis();
    }

    public static Date getNearlyTimeFromID(String msgID) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        byte[] bytes = UtilAll.string2bytes(msgID);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put(bytes, 10, 4);
        buf.position(0);
        long spanMS = buf.getLong();
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monStartTime = cal.getTimeInMillis();
        if (monStartTime + spanMS >= now) {
            cal.add(Calendar.MONTH, -1);
            monStartTime = cal.getTimeInMillis();
        }
        cal.setTimeInMillis(monStartTime + spanMS);
        return cal.getTime();
    }


    private static byte[] createFakeIP() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(System.currentTimeMillis());
        bb.position(4);
        byte[] fakeIP = new byte[4];
        bb.get(fakeIP);
        return fakeIP;
    }


    public static String createUniqueID() {
        StringBuilder sb = new StringBuilder(LEN * 2);
        sb.append(FIX_STRING);
        sb.append(UtilAll.bytes2string(createUniqueIDBuffer()));
        return sb.toString();
    }

    private static byte[] createUniqueIDBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 2);
        long current = System.currentTimeMillis();
        if (current >= nextStartTime) {
            setStartTime(current);
        }
        buffer.position(0);
        buffer.putInt((int) (System.currentTimeMillis() - startTime));
        buffer.putShort((short) COUNTER.getAndIncrement());
        return buffer.array();
    }

    public static void setUniqueID(final Message msg) {
        if (msg.getProperty(MessageConst.PROPERTY_UNIQUE_CLIENT_MESSAGE_ID_KEYIDX) == null) {
            msg.putProperty(MessageConst.PROPERTY_UNIQUE_CLIENT_MESSAGE_ID_KEYIDX, createUniqueID());
        }
    }

    public static String getUniqueID(final Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_UNIQUE_CLIENT_MESSAGE_ID_KEYIDX);
    }
}
