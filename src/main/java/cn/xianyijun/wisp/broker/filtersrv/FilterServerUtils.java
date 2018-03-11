package cn.xianyijun.wisp.broker.filtersrv;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@Slf4j
public class FilterServerUtils {

    public static void callShell(final String shellString) {
        Process process = null;
        try {
            String[] cmdArray = splitShellString(shellString);
            process = Runtime.getRuntime().exec(cmdArray);
            process.waitFor();
            log.info("CallShell: <{}> OK", shellString);
        } catch (Throwable e) {
            log.error("CallShell: readLine IOException, {}", shellString, e);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    private static String[] splitShellString(final String shellString) {
        return shellString.split(" ");
    }
}
