package cn.xianyijun.wisp.filter.utils;

import com.google.common.hash.Hashing;
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

    public int[] calcBitPositions(String str) {
        int[] bitPositions = new int[this.k];

        long hash64 = Hashing.murmur3_128().hashString(str, UTF_8).asLong();

        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);

        for (int i = 1; i <= this.k; i++) {
            int combinedHash = hash1 + (i * hash2);
            // Flip all the bits if it's negative (guaranteed positive number)
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            bitPositions[i - 1] = combinedHash % this.m;
        }

        return bitPositions;
    }


    public boolean isHit(String str, BitsArray bits) {
        return isHit(calcBitPositions(str), bits);
    }

    public boolean isHit(int[] bitPositions, BitsArray bits) {
        check(bits);
        boolean ret = bits.getBit(bitPositions[0]);
        for (int i = 1; i < bitPositions.length; i++) {
            ret &= bits.getBit(bitPositions[i]);
        }
        return ret;
    }

    public boolean isHit(BloomFilterData filterData, BitsArray bits) {
        if (!isValid(filterData)) {
            throw new IllegalArgumentException(
                    String.format("Bloom filter data may not belong to this filter! %s, %s",
                            filterData, this.toString())
            );
        }
        return isHit(filterData.getBitPos(), bits);
    }


    private double logMN(double m, double n) {
        return Math.log(n) / Math.log(m);
    }


    protected void check(BitsArray bits) {
        if (bits.getBitLength() != this.m) {
            throw new IllegalArgumentException(
                    String.format("Length(%d) of bits in BitsArray is not equal to %d!", bits.getBitLength(), this.m)
            );
        }
    }

    public boolean isValid(BloomFilterData filterData) {
        return filterData != null
                && filterData.getBitNum() == this.m
                && filterData.getBitPos() != null
                && filterData.getBitPos().length == this.k;
    }


    public void hashTo(BloomFilterData filterData, BitsArray bits) {
        if (!isValid(filterData)) {
            throw new IllegalArgumentException(
                    String.format("Bloom filter data may not belong to this filter! %s, %s",
                            filterData, this.toString())
            );
        }
        hashTo(filterData.getBitPos(), bits);
    }

    public void hashTo(int[] bitPositions, BitsArray bits) {
        check(bits);

        for (int i : bitPositions) {
            bits.setBit(i, true);
        }
    }

    public BloomFilterData generate(String str) {
        int[] bitPositions = calcBitPositions(str);

        return new BloomFilterData(bitPositions, this.m);
    }

}
