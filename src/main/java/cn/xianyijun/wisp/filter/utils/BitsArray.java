package cn.xianyijun.wisp.filter.utils;

import lombok.Getter;

/**
 * @author xianyijun
 */
@Getter
public class BitsArray {

    private byte[] bytes;
    private int bitLength;

    public static BitsArray create(int bitLength) {
        return new BitsArray(bitLength);
    }

    public static BitsArray create(byte[] bytes, int bitLength) {
        return new BitsArray(bytes, bitLength);
    }

    public static BitsArray create(byte[] bytes) {
        return new BitsArray(bytes);
    }

    private BitsArray(int bitLength) {
        this.bitLength = bitLength;
        // init bytes
        int temp = bitLength / Byte.SIZE;
        if (bitLength % Byte.SIZE > 0) {
            temp++;
        }
        bytes = new byte[temp];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) 0x00;
        }
    }

    private BitsArray(byte[] bytes, int bitLength) {
        if (bytes == null || bytes.length < 1) {
            throw new IllegalArgumentException("Bytes is empty!");
        }

        if (bitLength < 1) {
            throw new IllegalArgumentException("Bit is less than 1.");
        }

        if (bitLength < bytes.length * Byte.SIZE) {
            throw new IllegalArgumentException("BitLength is less than bytes.length() * " + Byte.SIZE);
        }

        this.bytes = new byte[bytes.length];
        System.arraycopy(bytes, 0, this.bytes, 0, this.bytes.length);
        this.bitLength = bitLength;
    }

    private BitsArray(byte[] bytes) {
        if (bytes == null || bytes.length < 1) {
            throw new IllegalArgumentException("Bytes is empty!");
        }

        this.bitLength = bytes.length * Byte.SIZE;
        this.bytes = new byte[bytes.length];
        System.arraycopy(bytes, 0, this.bytes, 0, this.bytes.length);
    }


    private int subscript(int bitPos) {
        return bitPos / Byte.SIZE;
    }

    private int position(int bitPos) {
        return 1 << bitPos % Byte.SIZE;
    }

    public boolean getBit(int bitPos) {
        checkBitPosition(bitPos, this);

        return (this.bytes[subscript(bitPos)] & position(bitPos)) != 0;
    }


    private void checkBitPosition(int bitPos, BitsArray bitsArray) {
        checkInitialized(bitsArray);
        if (bitPos > bitsArray.getBitLength()) {
            throw new IllegalArgumentException("BitPos is greater than " + bitLength);
        }
        if (bitPos < 0) {
            throw new IllegalArgumentException("BitPos is less than 0");
        }
    }

    private void checkInitialized(BitsArray bitsArray) {
        if (bitsArray.bytes == null) {
            throw new RuntimeException("Not initialized!");
        }
    }

}
