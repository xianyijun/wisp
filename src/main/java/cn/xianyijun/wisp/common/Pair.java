package cn.xianyijun.wisp.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The type Pair.
 *
 * @param <T1> the type parameter
 * @param <T2> the type parameter
 * @author xianyijun
 */
@Data
@AllArgsConstructor
public class Pair<T1, T2> {
    private T1 first;
    private T2 second;

}
