package com.sky.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 启动时清除Redis缓存
 * 防止旧格式数据导致序列化错误
 */
@Component
@Slf4j
public class CacheClearOnStartup implements ApplicationRunner {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 启动时清除Redis缓存 ==========");
        
        try {
            // 清除所有以 dishCache: 开头的key
            Set<String> dishKeys = redisTemplate.keys("dishCache:*");
            if (dishKeys != null && !dishKeys.isEmpty()) {
                redisTemplate.delete(dishKeys);
                log.info("清除 dishCache 缓存 {} 条", dishKeys.size());
            }
            
            // 清除所有以 setmealCache: 开头的key
            Set<String> setmealKeys = redisTemplate.keys("setmealCache:*");
            if (setmealKeys != null && !setmealKeys.isEmpty()) {
                redisTemplate.delete(setmealKeys);
                log.info("清除 setmealCache 缓存 {} 条", setmealKeys.size());
            }
            
            // 清除所有以 categoryCache: 开头的key
            Set<String> categoryKeys = redisTemplate.keys("categoryCache:*");
            if (categoryKeys != null && !categoryKeys.isEmpty()) {
                redisTemplate.delete(categoryKeys);
                log.info("清除 categoryCache 缓存 {} 条", categoryKeys.size());
            }
            
            log.info("========== Redis缓存清除完成 ==========");
        } catch (Exception e) {
            log.error("清除Redis缓存失败: {}", e.getMessage());
        }
    }
}
