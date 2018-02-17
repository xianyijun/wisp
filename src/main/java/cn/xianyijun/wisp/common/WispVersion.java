package cn.xianyijun.wisp.common;

/**
 * @author xianyijun
 */
public class WispVersion {
    public static final int CURRENT_VERSION = Version.V1_0_0_RELEASE.ordinal();

    public static String getVersionDesc(int value) {
        int length = Version.values().length;
        if (value >= length) {
            return Version.values()[length - 1].name();
        }

        return Version.values()[value].name();
    }

    public static Version value2Version(int value) {
        int length = Version.values().length;
        if (value >= length) {
            return Version.values()[length - 1];
        }

        return Version.values()[value];
    }

    public enum Version {
        /**
         * 当前Wisp Version
         */
        V1_0_0_RELEASE
    }
}
