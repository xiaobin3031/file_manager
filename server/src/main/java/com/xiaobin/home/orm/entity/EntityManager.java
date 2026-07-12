package com.xiaobin.home.orm.entity;

import com.xiaobin.home.orm.annotation.Column;
import com.xiaobin.home.orm.annotation.Entity;
import com.xiaobin.home.orm.annotation.Id;
import com.xiaobin.home.orm.config.OrmConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EntityManager implements ApplicationRunner {

    @Autowired
    private OrmConfig ormConfig;

    private static final Map<Class<?>, EntityMeta> entityMetaMap = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (CollectionUtils.isEmpty(ormConfig.getEntityPackages())) return;

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (String pkg : this.ormConfig.getEntityPackages()) {
            for (BeanDefinition def : scanner.findCandidateComponents(pkg)) {
                Class<?> clazz = Class.forName(def.getBeanClassName());
                EntityMeta entityMeta = parseEntityMeta(clazz);
                entityMetaMap.put(clazz, entityMeta);
            }
        }
    }

    public static EntityMeta getEntityMeta(Class<?> clazz) {
        EntityMeta entityMeta = entityMetaMap.get(clazz);
        if (entityMeta == null) {
            throw new RuntimeException("unknown EntityMeta of Class: " + clazz);
        }
        return entityMeta;
    }

    private EntityMeta parseEntityMeta(Class<?> entityClass) throws Exception {
        String tableName;
        Entity annotation = entityClass.getAnnotation(Entity.class);
        tableName = annotation.tableName();
        if (StringUtils.isEmpty(tableName)) {
            tableName = parseDbName(entityClass.getSimpleName());
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> cls = entityClass;
        List<ColumnMeta> columnMetas = new ArrayList<>();
        Set<String> fieldSet = new HashSet<>();
        while (!cls.getName().equals("java.lang.Object")) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                if (fieldSet.contains(field.getName())) continue;
                fieldSet.add(field.getName());
                String methodName = this.parseMethodName(field.getName());
                MethodHandle setter = lookup.findVirtual(entityClass, "set" + methodName, MethodType.methodType(void.class, field.getType()));
                MethodHandle getter;
                try {
                    getter = lookup.findVirtual(entityClass, "get" + methodName, MethodType.methodType(field.getType()));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    getter = lookup.findVirtual(entityClass, "is" + methodName, MethodType.methodType(field.getType()));
                }
                String column;
                Column colAnno = field.getAnnotation(Column.class);
                if (colAnno != null && StringUtils.isNotEmpty(colAnno.column())) {
                    column = colAnno.column();
                } else {
                    column = parseDbName(field.getName());
                }

                Id id = field.getAnnotation(Id.class);
                ColumnMeta columnMeta = new ColumnMeta(id != null, field.getName(), column, getter, setter, field.getType());
                columnMetas.add(columnMeta);
            }
            cls = cls.getSuperclass();
        }

        MethodHandle constructor = lookup.findConstructor(entityClass, MethodType.methodType(void.class));
        return new EntityMeta(constructor, tableName, columnMetas);
    }

    private String parseMethodName(String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private String parseDbName(String fieldName) {
        StringBuilder dbName = new StringBuilder(fieldName.length() + 5);
        char ch = fieldName.charAt(0);
        if (Character.isUpperCase(ch)) {
            dbName.append(Character.toLowerCase(ch));
        } else {
            dbName.append(ch);
        }
        for (int i = 1; i < fieldName.length(); i++) {
            ch = fieldName.charAt(i);
            if (Character.isUpperCase(ch)) {
                dbName.append("_").append(Character.toLowerCase(ch));
            } else {
                dbName.append(ch);
            }
        }
        return dbName.toString();
    }
}
