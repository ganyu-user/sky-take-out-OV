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
 * 2. 缓存一致性保障
 *    - CacheSyncService: 缓存同步服务，实现主动更新+超时删除策略
 *    - 支持缓存穿透、击穿、雪崩防护
 * 
 * 3. 缓存预热
 *    - CachePreheatService: 缓存预热服务，系统启动时预加载热点数据
 * 
 * 4. 缓存工具
 *    - CacheHelper: 缓存操作工具类
 * 
 * 5. 缓存监控
 *    - CacheController: 缓存管理接口，用于监控和管理缓存
 * 
 * 核心特性：
 * - 查询顺序：本地缓存(Caffeine) -> Redis缓存 -> 数据库
 * - 更新顺序：更新数据库 -> 删除Redis缓存 -> 删除本地缓存
 * - 缓存穿透防护：缓存空值
 * - 缓存击穿防护：分布式锁
 * - 缓存雪崩防护：随机过期时间
 * 
 * 使用方法：
 * 1. 在Controller方法上使用@Cacheable注解启用缓存
 * 2. 在数据变更方法上使用@CacheEvict注解清除缓存
 * 3. 使用CachePreheatService进行缓存预热
 * 4. 使用CacheController监控缓存状态
 * 
 * @author Sky Team
 * @since 1.0
 */
package com.sky.cache;
