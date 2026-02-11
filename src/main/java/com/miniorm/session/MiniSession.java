package com.miniorm.session;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniorm.cache.FirstLevelCache;
import com.miniorm.metadata.EntityMetadata;
import com.miniorm.query.SqlBuilder;

public class MiniSession implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(MiniSession.class);
    
    private final Connection connection;
    private final FirstLevelCache cache = new FirstLevelCache();
    private static final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();

    public MiniSession(DataSource dataSource) throws SQLException {
        this.connection = dataSource.getConnection();
        this.connection.setAutoCommit(false);
    }

    public void beginTransaction() {
        log.info("Transaction started");
    }

    public void commit() throws SQLException {
        connection.commit();
        log.info("Transaction committed");
    }

    public void rollback() {
        try {
            connection.rollback();
            log.warn("Transaction rolled back");
        } catch (SQLException e) {
            log.error("Failed to rollback", e);
        }
    }

    public <T> void save(T entity) throws Exception {
        EntityMetadata meta = getMetadata(entity.getClass());
        String sql = SqlBuilder.buildInsert(meta);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            for (Field field : meta.getColumns()) {
                stmt.setObject(i++, field.get(entity));
            }
            for (Field field : meta.getForeignKeys()) {
                Object relatedEntity = field.get(entity);
                if (relatedEntity != null) {
                    EntityMetadata relatedMeta = getMetadata(relatedEntity.getClass());
                    Object relatedId = relatedMeta.getIdField().get(relatedEntity);
                    stmt.setObject(i++, relatedId);
                } else {
                    stmt.setObject(i++, null);
                }
            }
            
            log.debug("Executing Save: {}", sql);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    meta.getIdField().set(entity, id);
                    cache.put(entity.getClass(), id, entity);
                }
            }
        }
    }

    public <T> void update(T entity) throws Exception {
        EntityMetadata meta = getMetadata(entity.getClass());
        String sql = SqlBuilder.buildUpdate(meta);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int i = 1;

            for (Field field : meta.getColumns()) {
                stmt.setObject(i++, field.get(entity));
            }
            for (Field field : meta.getForeignKeys()) {
                Object relatedEntity = field.get(entity);
                if (relatedEntity != null) {
                    EntityMetadata relatedMeta = getMetadata(relatedEntity.getClass());
                    Object relatedId = relatedMeta.getIdField().get(relatedEntity);
                    stmt.setObject(i++, relatedId);
                } else {
                    stmt.setObject(i++, null);
                }
            }
            Object id = meta.getIdField().get(entity);
            if (id == null) {
                throw new IllegalArgumentException("Cannot update entity without ID");
            }
            stmt.setObject(i, id);
            
            log.debug("Executing Update: {}", sql);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                log.warn("Update executed but 0 rows affected. Check if ID exists.");
            }
            
            cache.put(entity.getClass(), id, entity);
        }
    }

    public <T> void delete(T entity) throws Exception {
        EntityMetadata meta = getMetadata(entity.getClass());
        String sql = SqlBuilder.buildDelete(meta);
        Object id = meta.getIdField().get(entity);

        if (id == null) {
            throw new IllegalArgumentException("Cannot delete entity without ID");
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            log.debug("Executing Delete: {}", sql);
            stmt.executeUpdate();
            
            cache.remove(entity.getClass(), id);
        }
    }

    public <T> T find(Class<T> clazz, Object id) throws Exception {
        if (cache.contains(clazz, id)) {
            log.debug("Cache Hit for {} ID: {}", clazz.getSimpleName(), id);
            return cache.get(clazz, id);
        }

        EntityMetadata meta = getMetadata(clazz);
        String sql = SqlBuilder.buildSelectById(meta);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            log.debug("Executing Find: {}", sql);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(clazz, rs, meta);
                }
            }
        }
        return null;
    }

    private <T> T mapResultSetToEntity(Class<T> clazz, ResultSet rs, EntityMetadata meta) throws Exception {
        T entity = clazz.getDeclaredConstructor().newInstance();
        
        String idCol = meta.getColumnName(meta.getIdField());
        meta.getIdField().set(entity, rs.getObject(idCol));
        
        for (Field field : meta.getColumns()) {
            String colName = meta.getColumnName(field);
            Object value = rs.getObject(colName);
            field.set(entity, value);
        }
        
        for (Field field : meta.getForeignKeys()) {
            String colName = meta.getColumnName(field);
            Object fkValue = rs.getObject(colName);
            if (fkValue != null) {
                Object relatedEntity = find(field.getType(), fkValue);
                field.set(entity, relatedEntity);
            }
        }
        
        cache.put(clazz, meta.getIdField().get(entity), entity);
        return entity;
    }

    private EntityMetadata getMetadata(Class<?> clazz) {
        return metadataCache.computeIfAbsent(clazz, EntityMetadata::new);
    }
    
    public void createTable(Class<?> clazz) throws SQLException {
         EntityMetadata meta = getMetadata(clazz);
         String sql = SqlBuilder.buildCreateTable(meta);
         try (Statement stmt = connection.createStatement()) {
             stmt.execute(sql);
             log.info("Created table: {}", meta.getTableName());
         }
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            cache.clear();
        }
    }
}