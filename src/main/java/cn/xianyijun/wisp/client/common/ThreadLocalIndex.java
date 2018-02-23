package cn.xianyijun.wisp.client.common;

import lombok.ToString;

import java.util.Random;

/**
 * @author xianyijun
 */
@ToString
public class ThreadLocalIndex {
    private final ThreadLocal<Integer> threadLocalIndex = new ThreadLocal<>();
    private final Random random = new Random();

    public int getAndIncrement() {
        Integer index = this.threadLocalIndex.get();
        if (null == index) {
            index = Math.abs(random.nextInt());
            if (index < 0) {
                index = 0;
            }
            this.threadLocalIndex.set(index);
        }

        index = Math.abs(index + 1);
        if (index < 0) {
            index = 0;
        }

        this.threadLocalIndex.set(index);
        return index;
    }
}
