package com.xiaobin.home.node;

import com.xiaobin.home.dto.StorageLocation;
import com.xiaobin.home.node.entity.FmNode;
import com.xiaobin.home.orm.query.OrmQuery;

import java.util.List;

public interface NodeService {

    OrmQuery<FmNode, ?> query();

    FmNode create(FmNode fmNode);

    FmNode get(Long id);

    FmNode getByToken(String token);

    List<FmNode> children(FmNode fmNode);

    boolean addChild(FmNode fmNode, FmNode child);

    boolean remove(FmNode fmNode);

    FmNode nextSibling(FmNode fmNode);

    FmNode previousSibling(FmNode fmNode);

    boolean update(FmNode fmNode);

    FmNode copy(FmNode source, FmNode target);

    boolean hasChild(FmNode fmNode, FmNode child);

    boolean isSame(FmNode node1, FmNode node2);

    FmNode parent(FmNode fmNode);

    boolean moveTo(FmNode source, FmNode target);

    FmNode insertBefore(FmNode newNode, FmNode refNode);

    boolean exists(Long id);

    StorageLocation location(FmNode fmNode);

    String generateToken(FmNode fmNode);

}
