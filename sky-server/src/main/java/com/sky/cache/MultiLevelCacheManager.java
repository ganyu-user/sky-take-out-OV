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
 * 实现Spring Cache的CacheManager接口
 * 管理多个多级缓存实例（MultiLevelCache）
 *
 * 核心功能：
 * 1. 统一管理本地缓存（Caffeine）和分布式缓存（Redis）
 * 2. 使用双重检查锁（DCL）确保缓存实例的线程安全创建
 * 3. 提供缓存实例的获取和枚举功能
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Caffeine<Object, Object> caffeineBuilder;
    // 缓存容器：key=缓存名称（如dishCache），value=多级缓存实例
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    /**
     * 构造方法
     * 初始化缓存管理器，传入Redis模板和Caffeine构建器
     *
     * @param redisTemplate Redis操作模板，用于与Redis交互
     * @param caffeineBuilder Caffeine缓存构建器，用于创建本地缓存
     */
    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                   Caffeine<Object, Object> caffeineBuilder) {
        this.redisTemplate = redisTemplate;
        this.caffeineBuilder = caffeineBuilder;
    }

    /**
     * 获取指定名称的缓存实例
     * 使用双重检查锁（DCL）确保线程安全
     *
     * 执行流程：
     * 1. 先从缓存Map中获取，如果存在直接返回
     * 2. 如果不存在，加锁 synchronized
     * 3. 再次检查（双重检查），防止加锁期间其他线程已创建
     * 4. 如果确实不存在，创建新的MultiLevelCache实例
     * 5. 放入缓存Map并返回
     *
     * @param name 缓存标识符（如dishCache、setmealCache），不能为null
     * @return 缓存实例，如果不存在则创建新的
     */
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
     * 获取所有已创建的缓存名称
     * Spring框架内部通过此方法获取当前所有缓存的名字
     * 用于管理、监控、统计等场景
     *
     * @return 所有缓存名称的集合
     */
    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    /**
     * 创建多级缓存实例
     * 实例化MultiLevelCache，传入必要的依赖
     *
     * @param name 缓存名称
     * @return 新创建的多级缓存实例
     */
    private Cache createMultiLevelCache(String name) {
        log.info("【创建多级缓存】cacheName: {}", name);
        return new MultiLevelCache(name, redisTemplate, caffeineBuilder);
    }
}
