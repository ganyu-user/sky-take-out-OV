package com.sky.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多级缓存实现类
 * 第一层：Caffeine本地缓存（速度快，容量小）
 * 第二层：Redis分布式缓存（速度慢，容量大）
 * 
 * 查询顺序：本地缓存 -> Redis缓存 -> 数据库
 * 更新顺序：更新数据库 -> 删除Redis缓存 -> 删除本地缓存
 */
@Slf4j
public class MultiLevelCache implements org.springframework.cache.Cache {

    private final String name;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> localCache;
    
    // 分布式锁，防止缓存击穿
    private final ReentrantLock lock = new ReentrantLock();
    
    // Redis缓存过期时间（分钟）
    private static final long REDIS_EXPIRE_MINUTES = 30;
    // 空值缓存过期时间（分钟）- 防止缓存穿透
    private static final long NULL_VALUE_EXPIRE_MINUTES = 5;
    // 空值占位符
    public static final String NULL_VALUE = "CACHE_NULL_VALUE";

    public MultiLevelCache(String name, 
                          RedisTemplate<String, Object> redisTemplate,
                          Caffeine<Object, Object> caffeineBuilder) {
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.localCache = caffeineBuilder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        Object value = lookup(key);
        return value != null ? () -> value : null;
    }

    @Override
    @Nullable
    public <T> T get(Object key, Class<T> type) {
        Object value = lookup(key);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                "Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        // 先从缓存获取
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }
        
        // 使用分布式锁防止缓存击穿
        lock.lock();
        try {
            // 双重检查
            value = lookup(key);
            if (value != null) {
                return (T) value;
            }
            
            // 加载数据
            try {
                value = valueLoader.call();
            } catch (Exception ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }
            
            // 放入缓存
            put(key, value);
            return (T) value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查询缓存
     * 顺序：本地缓存 -> Redis缓存
     */
    @Nullable
    private Object lookup(Object key) {
        String cacheKey = generateKey(key);
        
        // 1. 查询本地缓存
        Object value = localCache.getIfPresent(cacheKey);
        if (value != null) {
            log.debug("Local cache hit - key: {}", cacheKey);
            return handleNullValue(value);
        }
        
        // 2. 查询Redis缓存
        value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.debug("Redis cache hit - key: {}", cacheKey);
            // 回填本地缓存
            localCache.put(cacheKey, value);
            return handleNullValue(value);
        }
        
        log.debug("Cache miss - key: {}", cacheKey);
        return null;
    }

    /**
     * 处理空值占位符
     */
    @Nullable
    private Object handleNullValue(Object value) {
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        return value;
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        String cacheKey = generateKey(key);
        
        if (value == null) {
            // 缓存空值，防止缓存穿透
            value = NULL_VALUE;
            redisTemplate.opsForValue().set(cacheKey, value, NULL_VALUE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(cacheKey, value, REDIS_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        // 同时更新本地缓存
        localCache.put(cacheKey, value);
        log.debug("Cache put - key: {}", cacheKey);
    }

    /**
     * 仅在key不存在时才放入缓存
     */
    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        String cacheKey = generateKey(key);

        // 先检查本地缓存
        Object localValue = localCache.getIfPresent(cacheKey);
        if (localValue != null) {
            return () -> localValue;
        }

        // 再检查Redis
        Object redisValue = redisTemplate.opsForValue().get(cacheKey);
        if (redisValue != null) {
            localCache.put(cacheKey, redisValue);
            return () -> redisValue;
        }

        // 放入缓存
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        String cacheKey = generateKey(key);
        
        // 删除Redis缓存
        redisTemplate.delete(cacheKey);
        
        // 删除本地缓存
        localCache.invalidate(cacheKey);
        
        log.debug("Cache evict - key: {}", cacheKey);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        String cacheKey = generateKey(key);
        
        // 删除Redis缓存
        Boolean deleted = redisTemplate.delete(cacheKey);
        
        // 删除本地缓存
        localCache.invalidate(cacheKey);
        
        return deleted != null && deleted;
    }

    @Override
    public void clear() {
        // 清空本地缓存
        localCache.invalidateAll();
        
        // 清空Redis缓存（通过模式匹配删除）
        String pattern = name + ":*";
        // 注意：生产环境慎用，可能阻塞Redis
        // 这里仅作为示例
        
        log.info("Cache cleared - name: {}", name);
    }

    @Override
    public boolean invalidate() {
        clear();
        return true;
    }

    /**
     * 生成缓存key
     * 格式：cacheName:key
     */
    private String generateKey(Object key) {
        return name + ":" + key.toString();
    }

    /**
     * 获取本地缓存统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalStats() {
        return localCache.stats();
    }

    /**
     * 获取本地缓存大小
     */
    public long getLocalSize() {
        return localCache.estimatedSize();
    }
}
