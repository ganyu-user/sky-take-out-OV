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
 */
@Configuration
@EnableCaching
@Slf4j
public class MultiLevelCacheConfig {

    /**
     * 配置Caffeine本地缓存
     * 用于存储热点数据，减少Redis访问压力
     */
    @Bean("localCacheBuilder")
    public Caffeine<Object, Object> localCacheBuilder() {
        return Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000)
                // 写入后60秒过期
                .expireAfterWrite(60, TimeUnit.SECONDS)
                // 访问后30秒过期
                .expireAfterAccess(30, TimeUnit.SECONDS)
                // 开启统计功能
                .recordStats()
                // 移除监听器
                .removalListener((key, value, cause) -> {
                    log.debug("Local cache removed - key: {}, cause: {}", key, cause);
                });
    }

    /**
     * 配置多级缓存管理器
     * 优先使用本地缓存(Caffeine)，其次使用Redis
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
            RedisTemplate<String, Object> redisTemplate,
            Caffeine<Object, Object> localCacheBuilder) {
        log.info("Initializing multi-level cache manager...");
        return new MultiLevelCacheManager(redisTemplate, localCacheBuilder);
    }
}
