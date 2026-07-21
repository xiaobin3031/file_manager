package com.xiaobin.home.node;

import com.xiaobin.home.constant.Constant;
import com.xiaobin.home.dto.StorageLocation;
import com.xiaobin.home.node.entity.FmNode;
import com.xiaobin.home.node.entity.FmStorage;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.orm.entity.ColumnMeta;
import com.xiaobin.home.orm.entity.EntityManager;
import com.xiaobin.home.orm.entity.EntityMeta;
import com.xiaobin.home.orm.query.OrmQuery;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalNodeService implements NodeService {

    private static final Map<String, Long> fileTokenMap = new ConcurrentHashMap<>();

    @Autowired
    private Orm orm;

    @Override
    public OrmQuery<FmNode, ?> query() {
        return this.orm.of(FmNode.class);
    }

    @Override
    public FmNode create(FmNode fmNode) {
        if (fmNode.getUpdateTime() == null) fmNode.setUpdateTime(LocalDateTime.now());
        if (fmNode.getCreateTime() == null) fmNode.setCreateTime(LocalDateTime.now());
        this.orm.save(fmNode);
        return fmNode;
    }

    @Override
    public FmNode get(Long id) {
        if (id == null || id <= 0) return null;
        return this.orm.of(FmNode.class).eq(FmNode::getId, id).eq(FmNode::getDeleted, 0).one();
    }

    @Override
    public FmNode getByToken(String token) {
        Long id = fileTokenMap.get(token);
        if (id != null) return get(id);
        return null;
    }

    @Override
    public List<FmNode> children(FmNode fmNode) {
        if (fmNode.getParentId() == null) return List.of();
        return this.orm.of(FmNode.class)
                .eq(FmNode::getParentId, fmNode.getParentId())
                .eq(FmNode::getOwnerId, fmNode.getOwnerId())
                .eq(FmNode::getDeleted, 0)
                .list();
    }

    @Override
    public boolean addChild(FmNode fmNode, FmNode child) {
        FmNode exist = this.query().eq(FmNode::getParentId, fmNode.getId()).eq(FmNode::getDeleted, 0).eq(FmNode::getNodeName, child.getNodeName()).one();
        if (exist != null) {
            child.setParentId(fmNode.getId());
        }
        child.setParentId(fmNode.getId());
        child.setId(null);
        return this.create(child).getId() != null;
    }

    @Override
    public boolean remove(FmNode fmNode) {
        fmNode.setDeleted(true);
        return this.update(fmNode);
    }

    @Override
    public FmNode nextSibling(FmNode fmNode) {
        OrmQuery<FmNode, ?> query = this.orm.of(FmNode.class)
                .eq(FmNode::getParentId, fmNode.getParentId()).eq(FmNode::getDeleted, 0);
        if (fmNode.getSort() != null) {
            query.gt(FmNode::getSort, fmNode.getSort())
                    .orderByAsc(FmNode::getSort);
        } else {
            query.gt(FmNode::getId, fmNode.getId())
                    .orderByAsc(FmNode::getId);
        }
        return query.one();
    }

    @Override
    public FmNode previousSibling(FmNode fmNode) {
        OrmQuery<FmNode, ?> query = this.orm.of(FmNode.class)
                .eq(FmNode::getParentId, fmNode.getParentId()).eq(FmNode::getDeleted, 0);
        if (fmNode.getSort() != null) {
            query.lt(FmNode::getSort, fmNode.getSort())
                    .orderByDesc(FmNode::getSort);
        } else {
            query.lt(FmNode::getId, fmNode.getId())
                    .orderByDesc(FmNode::getId);
        }
        return query.one();
    }

    @Override
    public boolean update(FmNode fmNode) {
        fmNode.setUpdateTime(LocalDateTime.now());
        return this.orm.updateById(fmNode) > 0;
    }

    @Override
    public FmNode copy(FmNode source, FmNode target) {
        EntityMeta entityMeta = EntityManager.getEntityMeta(FmNode.class);
        try {
            for (ColumnMeta column : entityMeta.getColumnMetas()) {
                column.getSetter().invoke(source, column.getGetter().invoke(target));
            }
        } catch (Throwable e) {
            throw new RuntimeException("copy error", e);
        }
        return target;
    }

    @Override
    public boolean hasChild(FmNode fmNode, FmNode child) {
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public boolean isSame(FmNode node1, FmNode node2) {
        return node1 != null && node2 != null && node1.getId().equals(node2.getId());
    }

    @Override
    public FmNode parent(FmNode fmNode) {
        return this.orm.of(FmNode.class)
                .eq(FmNode::getId, fmNode.getParentId()).eq(FmNode::getDeleted, 0).one();
    }

    @Override
    public boolean moveTo(FmNode source, FmNode target) {
        if (target.getNodeType() == Constant.NODE_TYPE_FOLDER) {
            return this.addChild(target, source);
        }
        return false;
    }

    @Override
    public FmNode insertBefore(FmNode newNode, FmNode refNode) {
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public boolean exists(Long id) {
        return this.orm.of(FmNode.class).eq(FmNode::getId, id).eq(FmNode::getDeleted, 0).exist();
    }

    @Override
    public StorageLocation location(FmNode fmNode) {
        if (fmNode != null && fmNode.getStorageId() != null) {
            FmStorage one = this.orm.of(FmStorage.class).eq(FmStorage::getId, fmNode.getStorageId()).one();
            if (one != null) {
                return new StorageLocation("", one.getPath());
            }
        }
        return null;
    }

    @Override
    public String generateToken(FmNode fmNode) {
        String token = UUID.randomUUID().toString().replace("-", "");
        fileTokenMap.put(token, fmNode.getId());
        return token;
    }
}
