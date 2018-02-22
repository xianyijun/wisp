package cn.xianyijun.wisp.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionUtils {

    public static Boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }
}
