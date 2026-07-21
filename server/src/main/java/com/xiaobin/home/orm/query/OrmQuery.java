package com.xiaobin.home.orm.query;

import com.xiaobin.home.exception.SimpleBizException;
import com.xiaobin.home.orm.Orm;
import com.xiaobin.home.orm.annotation.SFunction;
import com.xiaobin.home.orm.entity.ColumnMeta;
import com.xiaobin.home.orm.entity.EntityManager;
import com.xiaobin.home.orm.entity.EntityMeta;
import com.xiaobin.home.orm.entity.IPage;
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

public class OrmQuery<T, SELF extends OrmQuery<T, SELF>> {

    @Getter
    protected final EntityMeta entityMeta;
    @Getter
    private final List<Object> values;
    protected final StringBuilder where = new StringBuilder();
    protected final Orm orm;
    private final List<String> groupBy = new ArrayList<>();
    private final List<String> orderBy = new ArrayList<>();
    private Integer offset;
    private Integer limit;
    private String column = "*";

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    public OrmQuery(Class<T> cls, Orm orm) {
        this.entityMeta = EntityManager.getEntityMeta(cls);
        this.values = new ArrayList<>();
        this.orm = orm;
    }

    public void addGroup(String group) {
        this.groupBy.add(group);
    }

    public void addOrderBy(String orderBy) {
        this.orderBy.add(orderBy);
    }

    public SELF orderByDesc(SFunction<T, ?> func) {
        String column = this.resolveSFunction(func);
        this.orderBy.add(column + " desc");
        return self();
    }

    public SELF orderByAsc(SFunction<T, ?> func) {
        String column = this.resolveSFunction(func);
        this.orderBy.add(column + " asc");
        return self();
    }

    public String buildGetOneSql() {
        this.offset = null;
        this.limit = 1;
        return this.buildSql();
    }

    public String buildTotalSql() {
        List<String> cachedOrderBy = new ArrayList<>(this.orderBy);
        this.orderBy.clear();
        this.column = "1";
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
        return "select " + column + " from " + entityMeta.getTable() + " where " + this.where;
    }

    public String buildPage(int page, int limit) {
        this.offset = (page - 1) * limit;
        this.limit = limit;
        return this.buildSql();
    }

    public String buildExistSql() {
        this.offset = null;
        this.limit = 1;
        this.column = "1";
        return this.buildSql();
    }

    public SELF buildQueryByEntity(T t) {
        try {
            for (ColumnMeta columnMeta : this.entityMeta.getColumnMetas()) {
                Object value = columnMeta.getGetter().invoke(t);
                if (value != null) {
                    this.appendColumn(columnMeta.getColumnName(), "=", value);
                }
            }
        } catch (Throwable e) {
            throw new SimpleBizException("查询失败");
        }
        return self();
    }

    public SELF eq(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, "=", value);
        return self();
    }

    public SELF ne(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, "!=", value);
        return self();
    }

    public SELF in(SFunction<T, ?> func, Collection<?> values) {
        this.buildConditions(func, "IN", values);
        return self();
    }

    public SELF notIn(SFunction<T, ?> func, Collection<Object> values) {
        this.buildConditions(func, "NOT IN", values);
        return self();
    }

    public SELF isNull(SFunction<T, ?> func) {
        this.buildCondition(func, "IS NULL", null);
        return self();
    }

    public SELF isNotNull(SFunction<T, ?> func) {
        this.buildCondition(func, "IS NOT NULL", null);
        return self();
    }

    public SELF gt(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, ">", value);
        return self();
    }

    public SELF lt(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, "<", value);
        return self();
    }

    public SELF ge(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, ">=", value);
        return self();
    }

    public SELF le(SFunction<T, ?> func, Object value) {
        this.buildCondition(func, "<=", value);
        return self();
    }

    public SELF between(SFunction<T, ?> func, Object value1, Object value2) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" BETWEEN ? and ?");
        this.values.add(value1);
        this.values.add(value2);
        return self();
    }

    public SELF notBetween(SFunction<T, ?> func, Object value1, Object value2) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" NOT BETWEEN ? and ?");
        this.values.add(value1);
        this.values.add(value2);
        return self();
    }

    private void buildCondition(SFunction<T, ?> func, String op, Object value) {
        String column = this.resolveSFunction(func);
        this.appendColumn(column, op, value);
    }

    private void appendColumn(String column, String op, Object value) {
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" ").append(op).append(" ?");
        if (value != null) {
            this.where.append(" ?");
            this.values.add(value);
        }
    }

    private void buildConditions(SFunction<T, ?> func, String op, Collection<?> values) {
        String column = this.resolveSFunction(func);
        if (!this.values.isEmpty()) {
            this.where.append(" AND ");
        }
        this.where.append(column).append(" ").append(op).append(values.stream().map(a -> "?").collect(Collectors.joining(",")));
        this.values.addAll(values);
    }

    protected String resolveSFunction(Serializable lambda) {
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

    public T one() {
        return this.orm.getOne(this);
    }

    public List<T> list() {
        return this.orm.getList(this);
    }

    public IPage<T> page(int page, int limit) {
        return this.orm.page(this, page, limit);
    }

    public boolean exist() {
        return this.orm.exist(this);
    }

}
