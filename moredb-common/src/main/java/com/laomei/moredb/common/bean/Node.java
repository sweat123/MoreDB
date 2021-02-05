package com.laomei.moredb.common.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author luobo.hwz on 2021/02/03 11:50 PM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    private String id;

    private String hostname;

    private int port;
}
