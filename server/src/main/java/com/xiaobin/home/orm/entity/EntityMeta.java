package com.xiaobin.home.orm.entity;

import lombok.Getter;
import lombok.Setter;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityMeta {
    @Getter
    private final MethodHandle constructor;
    @Getter
    private final String table;
    @Getter
    private final List<ColumnMeta> columnMetas;

    @Setter
    @Getter
    private boolean init;

    private final Map<String, ColumnMeta> getterMap = new HashMap<>();

    public EntityMeta(MethodHandle constructor, String table, List<ColumnMeta> columnMetas) {
        this.constructor = constructor;
        this.table = table;
        this.columnMetas = columnMetas;
        for (ColumnMeta columnMeta : columnMetas) {
            getterMap.put(columnMeta.getFieldName(), columnMeta);
        }
    }

    public String getColumn(String methodName) {
        String fieldName;
        if (methodName.startsWith("is")) {
            fieldName = methodName.substring(2);
        } else {
            fieldName = methodName.substring(3);
        }
        fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        return this.getterMap.get(fieldName).getColumnName();
    }
}
