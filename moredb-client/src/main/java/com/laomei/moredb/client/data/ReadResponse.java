package com.laomei.moredb.client.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author luobo.hwz on 2021/02/04 11:22 AM
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ReadResponse extends Response {
    private Map<String, byte[]> data;

    public ReadResponse(final boolean success, final String msg, final Map<String, byte[]> data) {
        super(success, msg);
        this.data = data;
    }
}
