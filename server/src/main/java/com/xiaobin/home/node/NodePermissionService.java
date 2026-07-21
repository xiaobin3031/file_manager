package com.xiaobin.home.node;

import com.xiaobin.home.exception.SimpleBizException;
import com.xiaobin.home.node.entity.FmNode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodePermissionService {

    public List<FmNode> filterPermissions(List<FmNode> nodes, Integer loginId) {
        return nodes;
    }

    public FmNode checkPermission(FmNode node, Integer loginId) {
        if (!loginId.equals(node.getOwnerId())) {
            throw new SimpleBizException("no privileges to access");
        }
        return node;
    }
}
