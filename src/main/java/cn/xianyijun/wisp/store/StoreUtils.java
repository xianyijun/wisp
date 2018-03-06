package cn.xianyijun.wisp.store;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class StoreUtils {

    public static final long TOTAL_PHYSICAL_MEMORY_SIZE = getTotalPhysicalMemorySize();

    @SuppressWarnings("restriction")
    public static long getTotalPhysicalMemorySize() {
        long physicalTotal = 1024 * 1024 * 1024 * 24L;
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            physicalTotal = ((com.sun.management.OperatingSystemMXBean) operatingSystemMXBean).getTotalPhysicalMemorySize();
        }

        return physicalTotal;
    }
}
