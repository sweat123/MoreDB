package com.laomei.moredb.server.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.google.protobuf.ByteString;

/**
 * @author luobo.hwz on 2021/01/26 12:11 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pair {

    private String key;
    private ByteString value;

    public static Pair pair(final String key, final byte[] array) {
        return new Pair(key, ByteString.copyFrom(array));
    }
}
