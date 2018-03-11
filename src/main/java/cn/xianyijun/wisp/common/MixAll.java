package cn.xianyijun.wisp.common;

import cn.xianyijun.wisp.common.annotation.ImportantField;
import cn.xianyijun.wisp.utils.ArrayUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xianyijun
 */
@Slf4j
public class MixAll {
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final long MASTER_ID = 0L;
    public static final String WISP_HOME_ENV = "WISP_HOME";
    public static final String WISP_HOME_PROPERTY = "wisp.home.dir";
    public static final String NAME_SERVER_ADDR_PROPERTY = "wisp.name.server.addr";
    public static final String MESSAGE_COMPRESS_LEVEL = "wisp.message.compressLevel";
    public static final String NAME_SERVER_ADDR_ENV = "NAME_SERVER_ADDR";
    public static final String DEFAULT_TRACE_REGION_ID = "DefaultRegion";
    public static final String WS_DOMAIN_SUBGROUP = System.getProperty("wisp.name.server.domain.subgroup", "nsaddr");
    public static final String SELF_TEST_TOPIC = "SELF_TEST_TOPIC";
    public static final String DEFAULT_TOPIC = "TBW102";
    public static final String BENCHMARK_TOPIC = "BenchmarkTest";
    public static final String OFFSET_MOVED_EVENT = "OFFSET_MOVED_EVENT";
    public static final String TOOLS_CONSUMER_GROUP = "TOOLS_CONSUMER";
    public static final String FILTER_SERVER_CONSUMER_GROUP = "FILTER_SERVER_CONSUMER";
    public static final String SELF_TEST_CONSUMER_GROUP = "SELF_TEST_C_GROUP";
    public static final String ONS_HTTP_PROXY_GROUP = "CID_ONS-HTTP-PROXY";
    public static final String CID_ONS_API_PULL_GROUP = "CID_ONS_API_PULL";
    public static final String CID_ONS_API_PERMISSION_GROUP = "CID_ONS_API_PERMISSION";
    public static final String CID_ONS_API_OWNER_GROUP = "CID_ONS_API_OWNER_GROUP";
    public static final String RETRY_GROUP_TOPIC_PREFIX = "%RETRY%";
    public static final String CID_RMQ_SYS_PREFIX = "CID_RMQ_SYS_";
    public static final String DLQ_GROUP_TOPIC_PREFIX = "%DLQ%";
    public static final String DEFAULT_NAME_SERVER_ADDR_LOOKUP = "localhost:9876";
    public static final String WS_DOMAIN_NAME = System.getProperty("wisp.name.server.domain", DEFAULT_NAME_SERVER_ADDR_LOOKUP);

    public static final String DEFAULT_PRODUCER_GROUP = "DEFAULT_PRODUCER";
    public static final String CLIENT_INNER_PRODUCER_GROUP = "CLIENT_INNER_PRODUCER";
    public static final String UNIQUE_MSG_QUERY_FLAG = "_UNIQUE_KEY_QUERY";

    public static final String DEFAULT_CONSUMER_GROUP = "DEFAULT_CONSUMER";

    public static final String CONSUME_CONTEXT_TYPE = "ConsumeContextType";


    public static void properties2Object(final Properties properties, final Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("set")) {
                try {
                    String key = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                    String value = properties.getProperty(key);
                    if (value != null) {
                        Class<?>[] parameterType = method.getParameterTypes();
                        if (!ArrayUtils.isEmpty(parameterType)) {
                            String className = parameterType[0].getSimpleName();
                            Object arg;
                            if ("int".equals(className) || "Integer".equals(className)) {
                                arg = Integer.parseInt(value);
                            } else if ("long".equals(className) || "Long".equals(className)) {
                                arg = Long.parseLong(value);
                            } else if ("double".equals(className) || "Double".equals(className)) {
                                arg = Double.parseDouble(value);
                            } else if ("boolean".equals(className) || "Boolean".equals(className)) {
                                arg = Boolean.parseBoolean(value);
                            } else if ("float".equals(className) || "Float".equals(className)) {
                                arg = Float.parseFloat(value);
                            } else if ("String".equals(className)) {
                                arg = value;
                            } else {
                                continue;
                            }
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
        }
    }


    public static void printObjectProperties(final Object object) {
        printObjectProperties(object, false);
    }

    public static void printObjectProperties(final Object object, final boolean onlyImportantField) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;
                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                        if (null == value) {
                            value = "";
                        }
                    } catch (IllegalAccessException e) {
                        log.error("Failed to obtain object properties", e);
                    }

                    if (onlyImportantField) {
                        Annotation annotation = field.getAnnotation(ImportantField.class);
                        if (null == annotation) {
                            continue;
                        }
                    }

                    log.info(name + "=" + value);
                }
            }
        }
    }

    public static Properties object2Properties(final Object object) {
        Properties properties = new Properties();

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;
                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                    } catch (IllegalAccessException e) {
                        log.error("[MixAll] object2Properties Failed to handle properties", e);
                    }
                    if (value != null) {
                        properties.setProperty(name, value.toString());
                    }
                }
            }
        }
        return properties;
    }


    public static String getRetryTopic(final String consumerGroup) {
        return RETRY_GROUP_TOPIC_PREFIX + consumerGroup;
    }

    public static void deleteFile(final String fileName) {
        File file = new File(fileName);
        boolean result = file.delete();
        log.info(fileName + (result ? " delete OK" : " delete Failed"));
    }

    public static String file2String(final String fileName) throws IOException {
        File file = new File(fileName);
        return file2String(file);
    }

    private static String file2String(final File file) throws IOException {
        if (file.exists()) {
            byte[] data = new byte[(int) file.length()];
            boolean result;

            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                int len = inputStream.read(data);
                result = len == data.length;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }

            if (result) {
                return new String(data);
            }
        }
        return null;
    }

    public static void string2File(final String str, final String fileName) throws IOException {

        String tmpFile = fileName + ".tmp";
        string2FileNotSafe(str, tmpFile);

        String bakFile = fileName + ".bak";
        String prevContent = file2String(fileName);
        if (prevContent != null) {
            string2FileNotSafe(prevContent, bakFile);
        }

        File file = new File(fileName);
        file.delete();

        file = new File(tmpFile);
        file.renameTo(new File(fileName));
    }

    public static void string2FileNotSafe(final String str, final String fileName) throws IOException {

        File file = new File(fileName);
        File fileParent = file.getParentFile();
        if (fileParent != null) {
            fileParent.mkdirs();
        }
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(str);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    public static Properties string2Properties(final String str) {
        Properties properties = new Properties();
        try {
            InputStream in = new ByteArrayInputStream(str.getBytes(DEFAULT_CHARSET));
            properties.load(in);
        } catch (Exception e) {
            log.error("Failed to handle properties", e);
            return null;
        }

        return properties;
    }

    public static String properties2String(final Properties properties) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(entry.getKey().toString()).append("=").append(entry.getValue().toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public static String getWSAddr() {
        String wsDomainName = System.getProperty("wisp.name.server.domain", DEFAULT_NAME_SERVER_ADDR_LOOKUP);
        String wsDomainSubgroup = System.getProperty("wisp.name.server.domain.subgroup", "nsaddr");
        String wsAddr = "http://" + wsDomainName + ":8080/wisp/" + wsDomainSubgroup;
        if (wsDomainName.indexOf(":") > 0) {
            wsAddr = "http://" + wsDomainName + "/wisp/" + wsDomainSubgroup;
        }
        return wsAddr;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String brokerVIPChannel(final boolean isChange, final String brokerAddr) {
        if (isChange) {
            String[] ipAndPort = brokerAddr.split(":");
            return ipAndPort[0] + ":" + (Integer.parseInt(ipAndPort[1]) - 2);
        } else {
            return brokerAddr;
        }
    }

    public static boolean compareAndIncreaseOnly(final AtomicLong target, final long value) {
        long prev = target.get();
        while (value > prev) {
            boolean updated = target.compareAndSet(prev, value);
            if (updated) {
                return true;
            }
            prev = target.get();
        }
        return false;
    }

    public static boolean isSysConsumerGroup(final String consumerGroup) {
        return consumerGroup.startsWith(CID_RMQ_SYS_PREFIX);
    }

    public static String getDLQTopic(final String consumerGroup) {
        return DLQ_GROUP_TOPIC_PREFIX + consumerGroup;
    }
}