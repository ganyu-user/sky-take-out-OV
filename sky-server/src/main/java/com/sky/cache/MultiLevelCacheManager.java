package com.sky.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 多级缓存管理器
 * 管理本地缓存(Caffeine)和分布式缓存(Redis)的组合
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Caffeine<Object, Object> caffeineBuilder;
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate, 
                                   Caffeine<Object, Object> caffeineBuilder) {
        this.redisTemplate = redisTemplate;
        this.caffeineBuilder = caffeineBuilder;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        
        synchronized (cacheMap) {
            cache = cacheMap.get(name);
            if (cache == null) {
                cache = createMultiLevelCache(name);
                cacheMap.put(name, cache);
            }
        }
        return cache;
    }

    /**
     * 创建多级缓存实例
     */
    private Cache createMultiLevelCache(String name) {
        log.info("Creating multi-level cache: {}", name);
        return new MultiLevelCache(name, redisTemplate, caffeineBuilder);
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }
}
