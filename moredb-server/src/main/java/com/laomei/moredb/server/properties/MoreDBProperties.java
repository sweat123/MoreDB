package com.laomei.moredb.server.properties;

import com.laomei.moredb.common.bean.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author luobo.hwz on 2021/02/02 7:20 PM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoreDBProperties {

    private String groupId;

    private Node   node;

    private String dataPath;

    private List<Node> groupNodes;
}
