package com.laomei.moredb.server.util;

import java.nio.charset.Charset;

/**
 * @author luobo.hwz on 2021/01/19 16:54
 */
public class SerializeUtil {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static byte[] serialize(final String obj) {
        return obj.getBytes(UTF_8);
    }

    public static String deserialize(final byte[] bytes) {
        return new String(bytes, UTF_8);
    }
}
