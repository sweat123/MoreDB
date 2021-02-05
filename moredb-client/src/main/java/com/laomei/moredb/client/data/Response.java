package com.laomei.moredb.client.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author luobo.hwz on 2021/02/04 11:21 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Response {

    private boolean success;

    private String msg;
}
