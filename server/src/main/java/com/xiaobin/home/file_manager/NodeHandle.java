package com.xiaobin.home.file_manager;

import com.xiaobin.home.dto.StorageLocation;
import com.xiaobin.home.entity.FmNode;
import com.xiaobin.home.orm.query.OrmQuery;

import java.util.List;

/**
 * 节点操作
 */
public interface NodeHandle {

    short getType();

    OrmQuery<FmNode, ?> query(Integer ownId);

    FmNode create(FmNode fmNode);

    FmNode get(Long id, Integer ownId);

    List<FmNode> children(FmNode fmNode);

    FmNode addChild(FmNode fmNode, FmNode child);

    boolean remove(FmNode fmNode);

    FmNode nextSibling(FmNode fmNode);

    FmNode previousSibling(FmNode fmNode);

    boolean update(FmNode fmNode);

    FmNode copy(FmNode source);

    boolean hasChild(FmNode fmNode, FmNode child);

    FmNode parent(FmNode fmNode);

    boolean moveTo(FmNode source, FmNode target);

    void insertBefore(FmNode newNode, FmNode refNode);

    StorageLocation location(FmNode fmNode);

}
