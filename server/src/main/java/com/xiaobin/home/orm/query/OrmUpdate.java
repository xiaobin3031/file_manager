package com.xiaobin.home.orm.query;

import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.orm.annotation.SFunction;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class OrmUpdate<T> extends OrmQuery<T, OrmUpdate<T>> {

    @Getter
    private final List<Object> setValues;
    private final StringBuilder setSql;

    public OrmUpdate(Class<T> cls, Orm orm) {
        super(cls, orm);
        this.setValues = new ArrayList<>();
        this.setSql = new StringBuilder();
    }

    public OrmUpdate<T> set(SFunction<T, ?> sFunction, Object value) {
        String column = super.resolveSFunction(sFunction);
        if (!setValues.isEmpty()) {
            setSql.append(", ");
        }
        setSql.append(column).append(" = ?");
        setValues.add(value);
        return this;
    }

    public String buildSql() {
        return "UPDATE %s set %s where %s".formatted(super.entityMeta.getTable(), setSql.toString(), super.where.toString());
    }

    public int update() {
        return super.orm.update(this);
    }
}
