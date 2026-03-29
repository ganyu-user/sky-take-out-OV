package com.sky.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 缓存工具类
 * 提供便捷的缓存操作方法
 */
@Slf4j
@Component
public class CacheHelper {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheSyncService cacheSyncService;

    /**
     * 获取缓存值
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @return 缓存值
     */
    public Object get(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? wrapper.get() : null;
    }

    /**
     * 获取缓存值（带类型）
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param type      类型
     * @return 缓存值
     */
    public <T> T get(String cacheName, String key, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return null;
        }
        return cache.get(key, type);
    }

    /**
     * 获取缓存值，不存在则加载
     *
     * @param cacheName   缓存名称
     * @param key         缓存key
     * @param valueLoader 数据加载器
     * @return 缓存值
     */
    public <T> T get(String cacheName, String key, Callable<T> valueLoader) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            try {
                return valueLoader.call();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load value", e);
            }
        }
        return cache.get(key, valueLoader);
    }

    /**
     * 放入缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param value     缓存值
     */
    public void put(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 删除缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     */
    public void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
        // 同时删除Redis缓存
        cacheSyncService.evict(cacheName, key);
    }

    /**
     * 批量删除缓存（按模式）
     *
     * @param cacheName 缓存名称
     * @param pattern   key模式
     */
    public void evictByPattern(String cacheName, String pattern) {
        cacheSyncService.evictByPattern(cacheName, pattern);
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
        cacheSyncService.clear(cacheName);
    }

    /**
     * 获取本地缓存统计信息
     *
     * @param cacheName 缓存名称
     * @return 统计信息
     */
    public String getStats(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof MultiLevelCache) {
            MultiLevelCache multiLevelCache = (MultiLevelCache) cache;
            return String.format("Local Cache Stats: %s, Size: %d",
                    multiLevelCache.getLocalStats(),
                    multiLevelCache.getLocalSize());
        }
        return "Cache stats not available";
    }
}
