package com.laomei.moredb.client;

import com.laomei.moredb.common.bean.Node;
import lombok.Data;

import java.util.List;

/**
 * @author luobo.hwz on 2021/02/03 11:51 PM
 */
@Data
public class ClientProperties {

    private String groupId;

    private List<Node> nodes;
}
