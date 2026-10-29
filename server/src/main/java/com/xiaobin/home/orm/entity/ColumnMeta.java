package com.xiaobin.home.orm.entity;

import lombok.Getter;

import java.lang.invoke.MethodHandle;

public class ColumnMeta {

    @Getter
    private final String fieldName;
    @Getter
    private final String columnName;
    @Getter
    private final Class<?> javaType;
    @Getter
    private final MethodHandle getter;
    @Getter
    private final MethodHandle setter;
    @Getter
    private final boolean id;

    public ColumnMeta(boolean id, String fieldName, String columnName, MethodHandle getter, MethodHandle setter, Class<?> javaType) {
        this.id = id;
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.getter = getter;
        this.setter = setter;
        this.javaType = javaType;
    }
}
