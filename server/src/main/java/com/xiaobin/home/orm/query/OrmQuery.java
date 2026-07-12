package com.xiaobin.home.orm.query;

import com.xiaobin.home.orm.annotation.SFunction;
import com.xiaobin.home.orm.entity.EntityManager;
import com.xiaobin.home.orm.entity.EntityMeta;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class OrmQuery<T> {

    @Getter
    private final EntityMeta entityMeta;
    @Getter
    private final List<Object> values;
    private final List<String> groupBy = new ArrayList<>();
    private final List<String> orderBy = new ArrayList<>();
    private final StringBuilder where = new StringBuilder();
    private Integer offset;

    private Integer limit;

    public OrmQuery(Class<T> cls) {
        this.entityMeta = EntityManager.getEntityMeta(cls);
        this.values = new ArrayList<>();
    }

    public void addGroup(String group) {
        this.groupBy.add(group);
    }

    public void addOrderBy(String orderBy) {
        this.orderBy.add(orderBy);
    }

    public String buildGetOneSql() {
        this.offset = null;
        this.limit = 1;
        return this.buildSql();
    }

    public String buildTotalSql() {
        List<String> cachedOrderBy = new ArrayList<>(this.orderBy);
        this.orderBy.clear();
        String sql = "select count(*) from (" + this.buildSql() + ") a";
        this.orderBy.addAll(cachedOrderBy);
        return sql;
    }

    public String buildSql() {
        if (!groupBy.isEmpty()) {
            this.where.append(" GROUP BY ");
            this.where.append(StringUtils.join(groupBy, ","));
        }
        if (!orderBy.isEmpty()) {
            this.where.append(" ORDER BY ");
            this.where.append(StringUtils.join(orderBy, ","));
        }
        if (offset != null && limit != null) {
            this.where.append(" LIMIT ").append(offset).append(", ").append(limit);
        } else if (this.limit != null) {
            this.where.append(" LIMIT ").append(this.limit);
        }
        return "select * from " + entityMeta.getTable() + " where " + this.where;
    }

    public String buildPage(int page, int limit) {
        this.offset = (page - 1) * limit;
        this.limit = limit;
        return this.buildSql();
    }

    public OrmQuery<T> eq(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, "=", value);
        return this;
    }

    public OrmQuery<T> ne(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, "!=", value);
        return this;
    }

    public OrmQuery<T> in(SFunction<T, ?> func, Collection<Object> values) {
        this.buildConditions(func, "IN", values);
        return this;
    }

    public OrmQuery<T> notIn(SFunction<T, ?> func, Collection<Object> values) {
        this.buildConditions(func, "NOT IN", values);
        return this;
    }

    public OrmQuery<T> isNull(SFunction<T, ?> func) {
        this.buildCondition(func, "IS NULL");
        return this;
    }

    public OrmQuery<T> isNotNull(SFunction<T, ?> func) {
        this.buildCondition(func, "IS NOT NULL");
        return this;
    }

    public OrmQuery<T> between(SFunction<T, ?> func, Object value1, Object value2) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" BETWEEN ? and ?");
        this.values.add(value1);
        this.values.add(value2);
        return this;
    }

    public OrmQuery<T> notBetween(SFunction<T, ?> func, Object value1, Object value2) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" NOT BETWEEN ? and ?");
        this.values.add(value1);
        this.values.add(value2);
        return this;
    }

    private void buildCondition(SFunction<T, ?> func, String op, Object value) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" ").append(op).append(" ?");
        this.values.add(value);
    }

    private void buildCondition(SFunction<T, ?> func, String op) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" ").append(op);
    }

    private void buildConditions(SFunction<T, ?> func, String op, Collection<Object> values) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" ").append(op).append(values.stream().map(a -> "?").collect(Collectors.joining(",")));
        this.values.addAll(values);
    }

    private String resolveSFunction(Serializable lambda) {
        try {
            Method method = lambda.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            SerializedLambda invoke = (SerializedLambda) method.invoke(lambda);
            String implMethodName = invoke.getImplMethodName();
            return this.entityMeta.getColumn(implMethodName);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
