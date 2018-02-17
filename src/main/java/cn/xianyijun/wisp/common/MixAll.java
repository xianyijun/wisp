package cn.xianyijun.wisp.common;

import cn.xianyijun.wisp.common.annotation.ImportantField;
import cn.xianyijun.wisp.utils.ArrayUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;

/**
 * @author xianyijun
 */
@Slf4j
public class MixAll {
    public static final String WISP_HOME_ENV = "WISP_HOME";
    public static final String WISP_HOME_PROPERTY = "wisp.home.dir";

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

    public static String file2String(final String fileName) throws IOException {
        File file = new File(fileName);
        return file2String(file);
    }

    public static String file2String(final File file) throws IOException {
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
}