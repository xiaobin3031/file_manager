package com.xiaobin.home.file_manager;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NodeHandleRegistry {

    private final Map<Short, NodeHandle> handleMap;

    public NodeHandleRegistry(List<NodeHandle> handles) {
        this.handleMap = handles.stream()
                .collect(Collectors.toMap(NodeHandle::getType, Function.identity(), (k1, k2) -> k2));
    }

    public NodeHandle get(short type) {
        NodeHandle nodeHandle = this.handleMap.getOrDefault(type, this.handleMap.get(0));
        if (nodeHandle == null) {
            throw new IllegalArgumentException("unknown handle type: " + type);
        }

        return nodeHandle;
    }

}
