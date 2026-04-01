package com.sky.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.sky.cache.MultiLevelCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置类
 * 配置Caffeine本地缓存和Redis分布式缓存
 *
 * 配置说明：
 * 1. Caffeine：本地缓存，速度快，容量小，用于存储热点数据
 * 2. Redis：分布式缓存，速度较慢，容量大，用于多实例共享
 * 3. MultiLevelCacheManager：统一管理多级缓存
 */
@Configuration
@EnableCaching
@Slf4j
public class MultiLevelCacheConfiguration {

    /**
     * 配置Caffeine本地缓存构建器
     * Caffeine是Google Guava Cache的升级版，性能更好
     *
     * 配置参数说明：
     * - initialCapacity(100)：初始容量，减少扩容时的性能开销
     * - maximumSize(1000)：最大容量，超过后使用W-TinyLFU算法淘汰
     * - expireAfterWrite(60秒)：写入后60秒过期，防止数据长期不更新
     * - expireAfterAccess(30秒)：访问后30秒过期，热点数据保持更久
     * - recordStats()：开启统计，用于监控命中率
     * - removalListener()：缓存移除监听器，用于日志记录
     *
     * @return Caffeine缓存构建器
     */
    @Bean("localCacheBuilder")
    public Caffeine<Object, Object> localCacheBuilder() {
        return Caffeine.newBuilder()
                // 初始容量100，减少扩容时的性能开销
                .initialCapacity(100)
                // 最大容量1000，超过后使用W-TinyLFU算法淘汰
                .maximumSize(1000)
                // 写入后60秒过期，防止数据长期不更新
                .expireAfterWrite(60, TimeUnit.SECONDS)
                // 访问后30秒过期，热点数据保持更久
                .expireAfterAccess(30, TimeUnit.SECONDS)
                // 开启统计，用于监控命中率
                .recordStats()
                // 缓存移除监听器，记录移除原因
                .removalListener((key, value, cause) -> {
                    log.info("【本地缓存移除】key: {}, 原因: {}", key, cause);
                });
    }

    /**
     * 配置多级缓存管理器
     * 作为Spring Cache的默认CacheManager
     *
     * 工作原理：
     * 1. 优先查询本地缓存（Caffeine），命中直接返回
     * 2. 本地未命中查询Redis，命中后回填到本地缓存
     * 3. Redis未命中则查询数据库，结果写入两级缓存
     *
     * @param redisTemplate Redis操作模板，用于Redis缓存操作
     * @param localCacheBuilder Caffeine构建器，用于创建本地缓存
     * @return 多级缓存管理器实例
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
            RedisTemplate<String, Object> redisTemplate,
            Caffeine<Object, Object> localCacheBuilder
    ) {
        log.info("【初始化多级缓存管理器】...");
        return new MultiLevelCacheManager(redisTemplate, localCacheBuilder);
    }
}
