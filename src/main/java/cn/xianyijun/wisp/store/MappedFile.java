package cn.xianyijun.wisp.store;

/**
 * @author xianyijun
 * todo
 */
public class MappedFile extends ReferenceResource{
    @Override
    public boolean cleanup(long currentRef) {
        return false;
    }
}
