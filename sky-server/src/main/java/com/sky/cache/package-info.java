/**
 * 多级缓存组件包
 *
 * 本包提供了完整的多级缓存解决方案，包括：
 *
 * 1. 多级缓存架构
 *    - MultiLevelCache: 多级缓存实现类（Caffeine本地缓存 + Redis分布式缓存）
 *    - MultiLevelCacheManager: 多级缓存管理器
 *    - MultiLevelCacheConfig: 多级缓存配置类
 *
 * 2. 缓存预热
 *    - CachePreheatService: 缓存预热服务，系统启动时预加载热点数据
 *
 * 3. 使用方法
 *    在Service方法上使用Spring Cache注解：
 *    - @Cacheable: 查询缓存，未命中则执行方法并缓存结果
 *    - @CacheEvict: 清除缓存
 *    - @CachePut: 更新缓存
 *
 * 核心特性：
 * - 查询顺序：本地缓存(Caffeine) -> Redis缓存 -> 数据库
 * - 更新顺序：更新数据库 -> 删除Redis缓存 -> 删除本地缓存
 * - 缓存穿透防护：缓存空值
 * - 缓存击穿防护：分布式锁
 * - 缓存雪崩防护：随机过期时间
 *
 * @author Sky Team
 * @since 1.0
 */
package com.sky.cache;
