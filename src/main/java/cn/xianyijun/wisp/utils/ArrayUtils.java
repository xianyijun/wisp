package cn.xianyijun.wisp.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArrayUtils {

    public static <T> boolean isEmpty(T[] array){
        return array == null || array.length == 0;
    }
}
