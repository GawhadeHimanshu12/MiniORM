package com.miniorm.metadata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.miniorm.annotations.Column;
import com.miniorm.annotations.Entity;
import com.miniorm.annotations.Id;
import com.miniorm.annotations.JoinColumn;
import com.miniorm.annotations.ManyToOne;
import com.miniorm.annotations.Table;

public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    private final Field idField;
    private final List<Field> columns;
    private final List<Field> foreignKeys;

    public EntityMetadata(Class<?> clazz) {
        this.entityClass = clazz;

        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not an @Entity");
        }

        Table tableAnn = clazz.getAnnotation(Table.class);
        this.tableName = (tableAnn != null && !tableAnn.name().isEmpty()) ? tableAnn.name() : clazz.getSimpleName().toLowerCase();

        this.columns = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        Field tempId = null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Id.class)) {
                tempId = field;
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                foreignKeys.add(field);
            } else if (field.isAnnotationPresent(Column.class)) {
                columns.add(field);
            }
        }

        if (tempId == null) throw new RuntimeException("Entity " + clazz.getName() + " must have an @Id field");
        this.idField = tempId;
    }

    public String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            return field.getAnnotation(Column.class).name();
        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            return field.getAnnotation(JoinColumn.class).name();
        } else if (field.isAnnotationPresent(Id.class)) {
            return "id"; 
        }
        return field.getName(); 
    }

    public Class<?> getEntityClass() { return entityClass; }
    public String getTableName() { return tableName; }
    public Field getIdField() { return idField; }
    public List<Field> getColumns() { return columns; }
    public List<Field> getForeignKeys() { return foreignKeys; }
}