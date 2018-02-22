package cn.xianyijun.wisp.remoting.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The enum Serialize type.
 */
@Getter
@AllArgsConstructor
public enum SerializeType {
    /**
     * Json serialize type.
     */
    JSON((byte) 0),
    /**
     * Wisp serialize type.
     */
    WISP((byte) 1);

    private byte code;

    /**
     * Gets type by code.
     *
     * @param code the code
     * @return the type by code
     */
    public static SerializeType getTypeByCode(byte code) {
        for (SerializeType serializeType : SerializeType.values()) {
            if (serializeType.getCode() == code) {
                return serializeType;
            }
        }
        return null;
    }

}
