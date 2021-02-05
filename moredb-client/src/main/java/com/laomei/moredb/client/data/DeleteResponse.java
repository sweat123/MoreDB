package com.laomei.moredb.client.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author luobo.hwz on 2021/02/04 11:21 AM
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DeleteResponse extends Response {

    public DeleteResponse(final boolean success, final String msg) {
        super(success, msg);
    }
}
