package cn.xianyijun.wisp.filter.utils;

import lombok.Getter;

import java.nio.charset.Charset;

@Getter
public class BloomFilter {
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private int f = 10;
    private int n = 128;

    private int k;
    private int m;

    private BloomFilter(int f, int n) {
        if (f < 1 || f >= 100) {
            throw new IllegalArgumentException("f must be greater or equal than 1 and less than 100");
        }
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater than 0");
        }

        this.f = f;
        this.n = n;

        double errorRate = f / 100.0;
        this.k = (int) Math.ceil(logMN(0.5, errorRate));

        if (this.k < 1) {
            throw new IllegalArgumentException("Hash function num is less than 1, maybe you should change the value of error rate or bit num!");
        }

        this.m = (int) Math.ceil(this.n * logMN(2, 1 / errorRate) * logMN(2, Math.E));
        this.m = (int) (Byte.SIZE * Math.ceil(this.m / (Byte.SIZE * 1.0)));
    }

    public static BloomFilter createByFn(int f, int n) {
        return new BloomFilter(f, n);
    }

    private double logMN(double m, double n) {
        return Math.log(n) / Math.log(m);
    }
}
