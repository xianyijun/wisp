package cn.xianyijun.wisp.client.hook;

import cn.xianyijun.wisp.exception.ClientException;

/**
 * The interface Check forbidden hook.
 * @author xianyijun
 */
public interface CheckForbiddenHook {


    /**
     * Hook name string.
     *
     * @return the string
     */
    String hookName();

    /**
     * Check forbidden.
     *
     * @param context the context
     * @throws ClientException the client exception
     */
    void checkForbidden(final CheckForbiddenContext context) throws ClientException;
}
