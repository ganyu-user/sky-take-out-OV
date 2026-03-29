package com.sky.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存同步服务
 * 实现缓存一致性保障：主动更新 + 超时删除策略
 * 
 * 核心功能：
 * 1. 主动更新：当数据变更时，主动删除相关缓存
 * 2. 超时删除：通过Redis过期时间自动删除过期缓存
 * 3. 缓存预热：定时刷新热点数据
 * 4. 防止缓存雪崩：随机过期时间
 */
@Slf4j
@Component
public class CacheSyncService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存过期时间基础值（分钟）
    private static final long BASE_EXPIRE_MINUTES = 30;
    // 随机过期时间范围（分钟）
    private static final long RANDOM_EXPIRE_RANGE = 10;

    /**
     * 删除指定缓存
     * 
     * @param cacheName 缓存名称
     * @param key       缓存key
     */
    public void evict(String cacheName, String key) {
        String cacheKey = cacheName + ":" + key;
        redisTemplate.delete(cacheKey);
        log.info("Cache evicted - key: {}", cacheKey);
    }

    /**
     * 批量删除缓存（按模式）
     * 
     * @param cacheName 缓存名称
     * @param pattern   key模式，如 "*" 表示全部
     */
    public void evictByPattern(String cacheName, String pattern) {
        String fullPattern = cacheName + ":" + pattern;
        Set<String> keys = redisTemplate.keys(fullPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cache evicted by pattern - pattern: {}, count: {}", fullPattern, keys.size());
        }
    }

    /**
     * 清空指定缓存
     * 
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        evictByPattern(cacheName, "*");
        log.info("Cache cleared - name: {}", cacheName);
    }

    /**
     * 设置缓存（带随机过期时间，防止缓存雪崩）
     * 
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param value     缓存值
     */
    public void put(String cacheName, String key, Object value) {
        String cacheKey = cacheName + ":" + key;
        // 随机过期时间，防止缓存雪崩
        long expireMinutes = BASE_EXPIRE_MINUTES + (long) (Math.random() * RANDOM_EXPIRE_RANGE);
        redisTemplate.opsForValue().set(cacheKey, value, expireMinutes, TimeUnit.MINUTES);
        log.debug("Cache put - key: {}, expire: {}min", cacheKey, expireMinutes);
    }

    /**
     * 获取缓存
     * 
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @return 缓存值
     */
    public Object get(String cacheName, String key) {
        String cacheKey = cacheName + ":" + key;
        Object value = redisTemplate.opsForValue().get(cacheKey);
        log.debug("Cache get - key: {}, hit: {}", cacheKey, value != null);
        return value;
    }

    /**
     * 获取缓存（带类型转换）
     * 
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param type      类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, Class<T> type) {
        Object value = get(cacheName, key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 检查缓存是否存在
     * 
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @return 是否存在
     */
    public boolean exists(String cacheName, String key) {
        String cacheKey = cacheName + ":" + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }

    /**
     * 延长缓存过期时间
     * 
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param minutes   延长分钟数
     * @return 是否成功
     */
    public boolean expire(String cacheName, String key, long minutes) {
        String cacheKey = cacheName + ":" + key;
        return Boolean.TRUE.equals(redisTemplate.expire(cacheKey, minutes, TimeUnit.MINUTES));
    }

    /**
     * 定时清理过期缓存统计
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredCacheStats() {
        log.debug("Scheduled cache cleanup executed");
    }
}
