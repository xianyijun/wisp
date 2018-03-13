package cn.xianyijun.wisp.utils;

import cn.xianyijun.wisp.common.MixAll;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

/**
 * @author xianyijun
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IOUtils {

    public static String toString(InputStream input, String encoding) throws IOException {
        return (null == encoding) ? toString(new InputStreamReader(input, MixAll.DEFAULT_CHARSET)) : toString(new InputStreamReader(
                input, encoding));
    }

    public static String toString(Reader reader) throws IOException {
        CharArrayWriter sw = new CharArrayWriter();
        copy(reader, sw);
        return sw.toString();
    }

    private static long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1 << 12];
        long count = 0;
        for (int n = 0; (n = input.read(buffer)) >= 0; ) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
