package com.xiaobin.home.file_manager.handler;

import com.xiaobin.home.constant.NodeConstant;
import com.xiaobin.home.dto.StorageLocation;
import com.xiaobin.home.entity.FmNode;
import com.xiaobin.home.entity.FmStorage;
import com.xiaobin.home.file_manager.NodeHandle;
import com.xiaobin.home.file_manager.NodeHandleRegistry;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.orm.query.OrmQuery;
import com.xiaobin.home.service.FmNodeExtService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class LocalFileHandle implements NodeHandle {

    @Autowired
    private NodeHandleRegistry nodeHandleRegistry;
    @Autowired
    private Orm orm;
    @Autowired
    private FmNodeExtService fmNodeExtService;

    @Override
    public short getType() {
        return NodeConstant.NODE_FILE;
    }

    @Override
    public OrmQuery<FmNode, ?> query(Integer ownId) {
        return this.orm.of(FmNode.class)
                .eq(FmNode::getNodeType, getType())
                .eq(FmNode::getOwnerId, ownId)
                .eq(FmNode::getDeleted, 0);
    }

    @Override
    public FmNode create(FmNode fmNode) {
        fmNode.setId(null);
        this.orm.save(fmNode);
        if (fmNode.getId() != null) {
            this.fmNodeExtService.insertBefore(fmNode.getParentId(), null, fmNode.getId());
        }
        return fmNode;
    }

    @Override
    public FmNode get(Long id, Integer ownId) {
        if (id == null || id <= 0) return null;

        return this.query(ownId).eq(FmNode::getId, id).one();
    }

    @Override
    public List<FmNode> children(FmNode fmNode) {
        return List.of();
    }

    @Override
    public FmNode addChild(FmNode fmNode, FmNode child) {
        return null;
    }

    @Override
    public boolean remove(FmNode fmNode) {
        FmNode data = get(fmNode.getId(), fmNode.getOwnerId());
        if (data != null) {
            FmNode update = new FmNode();
            update.setId(data.getId());
            update.setDeleted(true);
            update.setUpdateTime(LocalDateTime.now());
            if (this.orm.updateById(update) > 0) {
                this.fmNodeExtService.removeChild(fmNode.getParentId(), fmNode.getId());
                return true;
            }
        }
        return false;
    }

    @Override
    public FmNode nextSibling(FmNode fmNode) {
        return Optional.ofNullable(this.fmNodeExtService.nextSibling(fmNode.getParentId(), fmNode.getId()))
                .map(a -> this.get(a, fmNode.getOwnerId()))
                .orElse(null);
    }

    @Override
    public FmNode previousSibling(FmNode fmNode) {
        return Optional.ofNullable(this.fmNodeExtService.previousSibling(fmNode.getParentId(), fmNode.getId()))
                .map(a -> this.get(a, fmNode.getOwnerId()))
                .orElse(null);
    }

    @Override
    public boolean update(FmNode fmNode) {
        if (fmNode.getId() == null) {
            return false;
        }
        return this.orm.updateById(fmNode) > 0;
    }

    @Override
    public FmNode copy(FmNode source) {
        if (source == null) {
            return null;
        }

        FmNode target = new FmNode();
        target.setNodeType(source.getNodeType());
        target.setNodeName(source.getNodeName());
        target.setParentId(source.getParentId());
        target.setOwnerId(source.getOwnerId());
        target.setHidden(source.getHidden());
        return target;
    }

    @Override
    public boolean hasChild(FmNode fmNode, FmNode child) {
        return false;
    }

    @Override
    public FmNode parent(FmNode fmNode) {
        if (fmNode == null) return null;

        return Optional.ofNullable(this.nodeHandleRegistry.get(NodeConstant.NODE_FOLD))
                .map(a -> a.get(fmNode.getParentId(), fmNode.getOwnerId())).orElse(null);
    }

    @Override
    public boolean moveTo(FmNode source, FmNode target) {
        if (!Objects.equals(source.getParentId(), target.getId()) && target.getNodeType() == NodeConstant.NODE_FOLD) {
            FmNode update = new FmNode();
            update.setId(source.getId());
            update.setParentId(target.getId());
            if (this.update(update)) {
                this.fmNodeExtService.removeChild(source.getParentId(), source.getId());
                this.fmNodeExtService.insertAfter(target.getId(), null, source.getId());
            }
        }
        return false;
    }

    @Override
    public void insertBefore(FmNode newNode, FmNode refNode) {
        if (Objects.equals(newNode.getParentId(), refNode.getParentId())) {
            this.fmNodeExtService.insertBefore(refNode.getParentId(), refNode.getId(), newNode.getId());
        }
    }

    @Override
    public StorageLocation location(FmNode fmNode) {
        if (fmNode.getStorageId() != null && fmNode.getStorageId() > 0) {
            FmStorage one = this.orm.of(FmStorage.class).eq(FmStorage::getId, fmNode.getStorageId()).one();
            if (one != null && StringUtils.isNotEmpty(one.getPath())) {
                return new StorageLocation(null, one.getPath());
            }
        }
        return null;
    }
}
