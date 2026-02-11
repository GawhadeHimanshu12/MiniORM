package com.miniorm.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.miniorm.metadata.EntityMetadata;

public class SqlBuilder {

    public static String buildInsert(EntityMetadata meta) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(meta.getTableName()).append(" (");
        
        List<String> colNames = new ArrayList<>();
        
        for (Field f : meta.getColumns()) {
            colNames.add(meta.getColumnName(f));
        }
        for (Field f : meta.getForeignKeys()) {
            colNames.add(meta.getColumnName(f));
        }

        sql.append(String.join(", ", colNames));
        sql.append(") VALUES (");
        sql.append(colNames.stream().map(c -> "?").collect(Collectors.joining(", ")));
        sql.append(")");
        
        return sql.toString();
    }

    public static String buildUpdate(EntityMetadata meta) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(meta.getTableName()).append(" SET ");
        
        List<String> sets = new ArrayList<>();
        
        for (Field f : meta.getColumns()) {
            sets.add(meta.getColumnName(f) + " = ?");
        }
        for (Field f : meta.getForeignKeys()) {
            sets.add(meta.getColumnName(f) + " = ?");
        }
        
        if (sets.isEmpty()) {
            throw new RuntimeException("No columns to update for entity: " + meta.getTableName());
        }
        
        sql.append(String.join(", ", sets));
        sql.append(" WHERE ").append(meta.getColumnName(meta.getIdField())).append(" = ?");
        
        return sql.toString();
    }

    public static String buildSelectById(EntityMetadata meta) {
        return "SELECT * FROM " + meta.getTableName() + " WHERE " + meta.getColumnName(meta.getIdField()) + " = ?";
    }

    public static String buildDelete(EntityMetadata meta) {
        return "DELETE FROM " + meta.getTableName() + " WHERE " + meta.getColumnName(meta.getIdField()) + " = ?";
    }
    
    public static String buildCreateTable(EntityMetadata meta) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + meta.getTableName() + " (");
        String idCol = meta.getColumnName(meta.getIdField());
        sql.append(idCol).append(" BIGINT AUTO_INCREMENT PRIMARY KEY, ");
        
        for (Field f : meta.getColumns()) {
            String colName = meta.getColumnName(f);
            String type = getSqlType(f.getType());
            sql.append(colName).append(" ").append(type).append(", ");
        }
        
        for (Field f : meta.getForeignKeys()) {
             String colName = meta.getColumnName(f);
             sql.append(colName).append(" BIGINT, ");
        }
        
        if (sql.toString().endsWith(", ")) {
            sql.setLength(sql.length() - 2);
        }
        
        sql.append(")");
        return sql.toString();
    }

    private static String getSqlType(Class<?> type) {
        if (type == int.class || type == Integer.class) return "INT";
        if (type == long.class || type == Long.class) return "BIGINT";
        if (type == double.class || type == Double.class) return "DOUBLE";
        if (type == String.class) return "VARCHAR(255)";
        return "VARCHAR(255)"; 
    }
}