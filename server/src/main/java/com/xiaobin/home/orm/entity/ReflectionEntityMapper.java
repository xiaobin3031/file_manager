package com.xiaobin.home.orm.entity;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ReflectionEntityMapper implements EntityMapper {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T map(ResultSet rs, EntityMeta meta) throws SQLException {
        T t;
        try {
            t = (T) meta.getConstructor().invoke();
            for (ColumnMeta column : meta.getColumnMetas()) {
                Object value = rs.getObject(column.getColumnName(), column.getJavaType());
                column.getSetter().invoke(t, value);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return t;
    }
}
