package com.xiaobin.home.orm;

import com.xiaobin.home.orm.entity.*;
import com.xiaobin.home.orm.executor.SqlExecutor;
import com.xiaobin.home.orm.query.OrmQuery;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

@Service
public class Orm {

    @Autowired
    private SqlExecutor sqlExecutor;
    @Resource(name = "entityMapper")
    private EntityMapper entityMapper;

    public <T> int save(T entity) {
        EntityMeta entityMeta = EntityManager.getEntityMeta(entity.getClass());
        StringBuilder insertSql = new StringBuilder("INSERT INTO ");
        insertSql.append(entityMeta.getTable()).append("(");
        StringBuilder valueSql = new StringBuilder("VALUES(");

        List<Object> values = new ArrayList<>();
        for (ColumnMeta columnMeta : entityMeta.getColumnMetas()) {
            Object invoke = this.invokeValue(columnMeta.getGetter(), entity);
            if (invoke != null) {
                insertSql.append(columnMeta.getColumnName()).append(",");
                valueSql.append("?").append(",");
                values.add(invoke);
            }
        }
        char ch = insertSql.charAt(insertSql.length() - 1);
        if (ch == ',') insertSql.setCharAt(insertSql.length() - 1, ')');
        else insertSql.append(')');
        ch = valueSql.charAt(valueSql.length() - 1);
        if (ch == ',') valueSql.setCharAt(valueSql.length() - 1, ')');
        else valueSql.append(')');

        insertSql.append(" ").append(valueSql);
        return this.sqlExecutor.update(insertSql.toString(), values.toArray());
    }

    public <T> int updateById(T entity) {
        EntityMeta entityMeta = EntityManager.getEntityMeta(entity.getClass());
        StringBuilder updateSql = new StringBuilder("UPDATE ");
        updateSql.append(entityMeta.getTable()).append(" SET ");
        List<Object> values = new ArrayList<>();
        for (ColumnMeta columnMeta : entityMeta.getColumnMetas()) {
            if (columnMeta.isId()) continue;
            Object invoke = this.invokeValue(columnMeta.getGetter(), entity);
            if (invoke != null) {
                updateSql.append(columnMeta.getColumnName()).append("=?,");
                values.add(invoke);
            }
        }
        char ch = updateSql.charAt(updateSql.length() - 1);
        if (ch == ',') updateSql.setCharAt(updateSql.length() - 1, ' ');
        else updateSql.append(' ');
        updateSql.append(" WHERE ");

        int valueIdx = 0;
        for (ColumnMeta columnMeta : entityMeta.getColumnMetas()) {
            if (columnMeta.isId()) {
                Object o = this.invokeValue(columnMeta.getGetter(), entity);
                if (o == null) continue;
                if (valueIdx > 0) updateSql.append(" AND ");
                valueIdx++;
                updateSql.append(columnMeta.getColumnName()).append("=?");
                values.add(o);
            }
        }
        return this.sqlExecutor.update(updateSql.toString(), values.toArray());
    }

    public int delete(Class<?> clazz, Object... ids) {
        return 0;
    }

    public <T> OrmQuery<T> of(Class<T> cls) {
        return new OrmQuery<>(cls);
    }

    public <T> T getOne(OrmQuery<T> query) {
        String s = query.buildGetOneSql();
        List<Object> values = query.getValues();
        List<T> list = this.sqlExecutor.query(s, this.entityMapper, query.getEntityMeta(), values.toArray(new Object[0]));
        return list.isEmpty() ? null : list.getFirst();
    }

    public <T> List<T> getList(OrmQuery<T> query) {
        String s = query.buildGetOneSql();
        List<Object> values = query.getValues();
        return this.sqlExecutor.query(s, this.entityMapper, query.getEntityMeta(), values.toArray(new Object[0]));
    }

    public <T> IPage<T> page(OrmQuery<T> query, int page, int limit) {
        String sql = query.buildTotalSql();
        int total = this.sqlExecutor.queryTotal(sql, query.getValues());
        IPage<T> iPage = new IPage<>(total, page, limit);
        if (iPage.hasMore()) {
            String s = query.buildPage(page, limit);
            List<Object> values = query.getValues();
            List<T> list = this.sqlExecutor.query(s, this.entityMapper, query.getEntityMeta(), values.toArray(new Object[0]));
            iPage.setRecords(list);
        }
        return iPage;
    }

    private Object invokeValue(MethodHandle getMethod, Object entity) {
        Object invoke;
        try {
            invoke = getMethod.invoke(entity);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return invoke;
    }
}
