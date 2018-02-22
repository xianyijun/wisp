package cn.xianyijun.wisp.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
