package com.miniorm.cache;

import java.util.HashMap;
import java.util.Map;

public class FirstLevelCache {
    private final Map<Class<?>, Map<Object, Object>> cache = new HashMap<>();

    public void put(Class<?> clazz, Object id, Object entity) {
        cache.computeIfAbsent(clazz, k -> new HashMap<>()).put(id, entity);
    }

    public <T> T get(Class<T> clazz, Object id) {
        Map<Object, Object> classCache = cache.get(clazz);
        if (classCache != null) {
            return clazz.cast(classCache.get(id));
        }
        return null;
    }
    
    public void remove(Class<?> clazz, Object id) {
        if (cache.containsKey(clazz)) {
            cache.get(clazz).remove(id);
        }
    }

    public void clear() {
        cache.clear();
    }
    
    public boolean contains(Class<?> clazz, Object id) {
        return get(clazz, id) != null;
    }
}