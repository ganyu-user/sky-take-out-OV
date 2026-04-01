package com.sky.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.util.Random;
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
 *
 * 防护策略：
 * 1. 缓存穿透：缓存空值（NULL_VALUE）
 * 2. 缓存击穿：排他锁（ReentrantLock）
 * 3. 缓存雪崩：随机过期时间（30-40分钟）
 */
@Slf4j
public class MultiLevelCache implements org.springframework.cache.Cache {

    private final String name;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> localCache;

    // 分布式锁，防止缓存击穿
    private final ReentrantLock reentrantLock = new ReentrantLock();

    // Redis缓存过期时间（分钟）- 基础值
    private static final long REDIS_EXPIRE_MINUTES = 30;
    // Redis缓存过期时间随机范围（分钟）- 防止缓存雪崩
    private static final long REDIS_EXPIRE_RANDOM_MINUTES = 10;
    // 空值缓存过期时间（分钟）- 防止缓存穿透
    private static final long NULL_VALUE_EXPIRE_MINUTES = 5;
    // 空值占位符
    public static final String NULL_VALUE = "CACHE_NULL_VALUE";
    // 随机数生成器
    private final Random random = new Random();

    /**
     * 构造方法
     * 创建多级缓存实例，初始化本地缓存和Redis连接
     *
     * @param name 缓存名称，用于区分不同的缓存（如dishCache、setmealCache）
     * @param redisTemplate Redis操作模板，用于与Redis交互
     * @param caffeineBuilder Caffeine缓存构建器，用于创建本地缓存
     */
    public MultiLevelCache(String name,
                          RedisTemplate<String, Object> redisTemplate,
                          Caffeine<Object, Object> caffeineBuilder) {
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.localCache = caffeineBuilder.build();
    }

    /**
     * 获取缓存名称
     * Spring Cache规范要求实现的方法
     *
     * @return 缓存名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取底层缓存实现
     * Spring Cache规范要求实现的方法
     *
     * @return 当前缓存实例
     */
    @Override
    public Object getNativeCache() {
        return this;
    }

    /**
     * 根据key获取缓存值
     * Spring Cache规范要求实现的方法
     *
     * @param key 缓存key
     * @return 缓存值的包装对象，如果不存在返回null
     */
    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        Object value = lookup(key);
        return value != null ? () -> value : null;
    }

    /**
     * 根据key获取缓存值，并转换为指定类型
     * Spring Cache规范要求实现的方法
     *
     * @param key 缓存key
     * @param type 期望返回的类型
     * @return 缓存值，如果不存在或类型不匹配返回null
     * @throws IllegalStateException 如果缓存值类型与期望类型不匹配
     */
    @Override
    @Nullable
    public <T> T get(Object key, Class<T> type) {
        Object value = lookup(key);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                "缓存值类型不匹配，期望类型 [" + type.getName() + "], 实际值: " + value);
        }
        return (T) value;
    }

    /**
     * 根据key获取缓存值，如果不存在则使用valueLoader加载并放入缓存
     * 这是Spring Cache的核心方法，@Cacheable注解最终调用此方法
     *
     * 防止缓存击穿逻辑：
     * 1. 先查询缓存，命中直接返回
     * 2. 未命中则加锁（ReentrantLock）
     * 3. 再次查询缓存（双重检查，防止加锁期间其他线程已放入缓存）
     * 4. 仍未命中则调用valueLoader加载数据
     * 5. 将数据放入缓存并返回
     *
     * @param key 缓存key
     * @param valueLoader 数据加载器，用于从数据库加载数据
     * @return 缓存值或从数据库加载的值
     * @throws ValueRetrievalException 如果数据加载失败
     */
    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        // 从缓存获取
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }

        // 缓存没有，加锁防止击穿
        reentrantLock.lock();
        try {
            // 再次从缓存中获取（双重检查）
            value = lookup(key);
            if (value != null) {
                return (T) value;
            }

            // 加锁后还是没有，加载数据
            try {
                value = valueLoader.call();
            } catch (Exception ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }

            // 将加载的数据放入缓存
            put(key, value);
            return (T) value;
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * 查询缓存的核心方法
     * 查询顺序：本地缓存（Caffeine）-> Redis缓存 -> 返回null
     *
     * 如果Redis缓存命中，会回填到本地缓存，下次查询直接从本地缓存获取
     *
     * @param key 缓存key
     * @return 缓存值，如果不存在返回null
     */
    @Nullable
    private Object lookup(Object key) {
        String cacheKey = generateKey(key);

        // 1. 查询本地缓存（L1）
        Object value = localCache.getIfPresent(cacheKey);
        if (value != null) {
            log.info("【本地缓存命中】cacheName: {}, key: {}", name, key);
            return handleNullValue(value);
        }

        // 2. 查询Redis缓存（L2）
        value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.info("【Redis缓存命中】cacheName: {}, key: {}", name, key);
            // 回填本地缓存，下次直接从本地获取
            localCache.put(cacheKey, value);
            return handleNullValue(value);
        }

        log.info("【缓存未命中】cacheName: {}, key: {}", name, key);
        return null;
    }

    /**
     * 处理空值占位符
     * 如果缓存中存储的是NULL_VALUE占位符，表示数据库中不存在该数据
     * 返回null给调用方，防止重复查询数据库（缓存穿透防护）
     *
     * @param value 缓存中的值
     * @return 实际值，如果是占位符则返回null
     */
    @Nullable
    private Object handleNullValue(Object value) {
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 将数据放入缓存
     * 同时放入本地缓存和Redis缓存
     *
     * 防止缓存雪崩：
     * Redis缓存过期时间使用随机值（30-40分钟），避免大量缓存同时过期
     *
     * 防止缓存穿透：
     * 如果value为null，缓存空值占位符（NULL_VALUE），5分钟后过期
     *
     * @param key 缓存key
     * @param value 缓存值，可以为null
     */
    @Override
    public void put(Object key, @Nullable Object value) {
        String cacheKey = generateKey(key);

        if (value == null) {
            // 缓存空值，防止缓存穿透
            value = NULL_VALUE;
            redisTemplate.opsForValue().set(cacheKey, value, NULL_VALUE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } else {
            // 随机过期时间，防止缓存雪崩（30-40分钟之间随机）
            long expireMinutes = REDIS_EXPIRE_MINUTES + random.nextInt((int) REDIS_EXPIRE_RANDOM_MINUTES);
            redisTemplate.opsForValue().set(cacheKey, value, expireMinutes, TimeUnit.MINUTES);
            log.debug("【缓存写入】key: {}, 过期时间: {}分钟", cacheKey, expireMinutes);
        }

        // 同时更新本地缓存
        localCache.put(cacheKey, value);
    }

    /**
     * 仅在key不存在时才放入缓存
     * 用于防止多个线程同时写入相同的缓存key
     *
     * 检查顺序：本地缓存 -> Redis缓存
     * 如果任意一层存在，则返回已存在的值，不写入新值
     * 如果都不存在，则写入新值
     *
     * @param key 缓存key
     * @param value 缓存值
     * @return 如果key已存在返回已存在的值，否则返回null
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

    /**
     * 删除指定key的缓存
     * 同时删除本地缓存和Redis缓存
     *
     * @param key 要删除的缓存key
     */
    @Override
    public void evict(Object key) {
        String cacheKey = generateKey(key);

        // 删除Redis缓存
        redisTemplate.delete(cacheKey);

        // 删除本地缓存
        localCache.invalidate(cacheKey);

        log.debug("【缓存删除】key: {}", cacheKey);
    }

    /**
     * 如果key存在则删除
     * 与evict的区别是返回是否实际删除了数据
     *
     * @param key 要删除的缓存key
     * @return 如果key存在并被删除返回true，否则返回false
     */
    @Override
    public boolean evictIfPresent(Object key) {
        String cacheKey = generateKey(key);

        // 删除Redis缓存
        Boolean deleted = redisTemplate.delete(cacheKey);

        // 删除本地缓存
        localCache.invalidate(cacheKey);

        return deleted != null && deleted;
    }

    /**
     * 清空所有缓存
     * 用于@CacheEvict(allEntries = true)场景
     *
     * 清空策略：
     * 1. 清空本地缓存（Caffeine.invalidateAll）
     * 2. 使用SCAN命令扫描Redis中匹配的key，批量删除
     *
     * 注意：使用SCAN而不是KEYS命令，避免阻塞Redis
     *
     * @CacheEvict(allEntries = true) 触发此方法
     */
    @Override
    public void clear() {
        // 清空本地缓存
        localCache.invalidateAll();

        // 清空Redis缓存（通过模式匹配删除）
        String pattern = name + ":*";
        try {
            // 使用scan命令查找匹配的key，然后批量删除
            org.springframework.data.redis.core.Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build()
                )
            );

            if (cursor != null) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    redisTemplate.delete(key);
                }
                cursor.close();
            }
            log.info("【缓存清空完成】cacheName: {}, 匹配模式: {}", name, pattern);
        } catch (Exception e) {
            log.error("【缓存清空失败】cacheName: {}, 错误信息: {}", name, e.getMessage());
        }
    }

    /**
     * 清空所有缓存并返回是否成功
     * 与clear的区别是返回操作结果
     *
     * @return 总是返回true
     */
    @Override
    public boolean invalidate() {
        clear();
        return true;
    }

    /**
     * 生成缓存key
     * 格式：cacheName:key
     * 例如：dishCache:32
     *
     * @param key 原始key
     * @return 带缓存名称前缀的完整key
     */
    private String generateKey(Object key) {
        return name + ":" + key.toString();
    }

    /**
     * 获取本地缓存统计信息
     * 用于监控Caffeine缓存的命中率、加载次数等
     *
     * @return Caffeine缓存统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalStats() {
        return localCache.stats();
    }

    /**
     * 获取本地缓存当前大小
     * 用于监控Caffeine缓存的条目数量
     *
     * @return 本地缓存中的条目数
     */
    public long getLocalSize() {
        return localCache.estimatedSize();
    }
}
