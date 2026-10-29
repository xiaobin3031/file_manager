package com.xiaobin.home.service;

import com.xiaobin.home.entity.FmNodeExt;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.orm.query.OrmQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class FmNodeExtService {

    @Autowired
    private Orm orm;

    private OrmQuery<FmNodeExt, ?> query() {
        return this.orm.of(FmNodeExt.class)
                .eq(FmNodeExt::getDeleted, 0);
    }

    public FmNodeExt get(Long foldId) {
        return this.query().eq(FmNodeExt::getFoldId, foldId).one();
    }

    public void removeChild(Long foldId, Long nodeId) {
        FmNodeExt fmNodeExt = get(foldId);
        if (fmNodeExt != null && StringUtils.isNotEmpty(fmNodeExt.getChildrenOrder())) {
            String[] split = fmNodeExt.getChildrenOrder().split(",");
            String nodeIdStr = String.valueOf(nodeId);
            List<String> list = Arrays.stream(split).filter(a -> !a.equals(nodeIdStr)).toList();
            FmNodeExt update = new FmNodeExt();
            update.setId(fmNodeExt.getId());
            update.setChildrenOrder(String.join(",", list));
            this.orm.updateById(update);
        }
    }

    public void insertBefore(Long foldId, Long refNodeId, Long targetNodeId) {
        FmNodeExt fmNodeExt = get(foldId);
        if (fmNodeExt != null && StringUtils.isNotEmpty(fmNodeExt.getChildrenOrder())) {
            String[] split = fmNodeExt.getChildrenOrder().split(",");
            List<String> list = new ArrayList<>(List.of(split));
            if (refNodeId == null) {
                list.add(String.valueOf(targetNodeId));
            } else {
                String nodeIdStr = String.valueOf(refNodeId);
                int idx = IntStream.range(0, split.length)
                        .filter(i -> split[i].equals(nodeIdStr))
                        .findFirst()
                        .orElse(-1);
                if (idx == -1) {
                    list.add(String.valueOf(targetNodeId));
                } else {
                    list.add(idx, String.valueOf(targetNodeId));
                }
            }
            FmNodeExt update = new FmNodeExt();
            update.setId(fmNodeExt.getId());
            update.setChildrenOrder(String.join(",", list));
            this.orm.updateById(update);
        }
    }

    public void insertAfter(Long foldId, Long refNodeId, Long targetNodeId) {
        FmNodeExt fmNodeExt = get(foldId);
        if (fmNodeExt != null && StringUtils.isNotEmpty(fmNodeExt.getChildrenOrder())) {
            String[] split = fmNodeExt.getChildrenOrder().split(",");
            List<String> list = new ArrayList<>(List.of(split));
            if (refNodeId == null) {
                list.add(String.valueOf(targetNodeId));
            } else {
                String nodeIdStr = String.valueOf(refNodeId);
                int idx = IntStream.range(0, split.length)
                        .filter(i -> split[i].equals(nodeIdStr))
                        .findFirst()
                        .orElse(-1);
                if (idx == -1 || idx == split.length - 1) {
                    list.add(String.valueOf(targetNodeId));
                } else {
                    list.add(idx + 1, String.valueOf(targetNodeId));
                }
            }
            FmNodeExt update = new FmNodeExt();
            update.setId(fmNodeExt.getId());
            update.setChildrenOrder(String.join(",", list));
            this.orm.updateById(update);
        }
    }

    public Long nextSibling(Long foldId, Long refNodeId) {
        return Optional.ofNullable(get(foldId))
                .map(FmNodeExt::getChildrenOrder)
                .filter(StringUtils::isNotEmpty)
                .map(a -> List.of(a.split(",")))
                .map(list -> {
                    int idx = list.indexOf(String.valueOf(refNodeId));
                    if (idx == -1 || idx == list.size() - 1) {
                        return null;
                    }
                    return Long.parseLong(list.get(idx + 1));
                })
                .orElse(null);
    }

    public Long previousSibling(Long foldId, Long refNodeId) {
        return Optional.ofNullable(get(foldId))
                .map(FmNodeExt::getChildrenOrder)
                .filter(StringUtils::isNotEmpty)
                .map(a -> List.of(a.split(",")))
                .map(list -> {
                    int idx = list.indexOf(String.valueOf(refNodeId));
                    if (idx <= 0) {
                        return null;
                    }
                    return Long.parseLong(list.get(idx - 1));
                })
                .orElse(null);
    }

}
