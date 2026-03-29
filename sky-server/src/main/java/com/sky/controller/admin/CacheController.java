package com.sky.controller.admin;

import com.sky.cache.CacheHelper;
import com.sky.cache.CachePreheatService;
import com.sky.cache.CacheSyncService;
import com.sky.cache.MultiLevelCache;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理接口
 * 用于监控和管理多级缓存
 */
@RestController
@RequestMapping("/admin/cache")
@Slf4j
@Api(tags = "Cache(缓存管理接口)")
public class CacheController {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheSyncService cacheSyncService;

    @Autowired
    private CachePreheatService cachePreheatService;

    @Autowired
    private CacheHelper cacheHelper;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    @ApiOperation("获取缓存统计信息")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取各缓存的统计信息
        stats.put("dishCache", cacheHelper.getStats(CachePreheatService.DISH_CACHE));
        stats.put("setmealCache", cacheHelper.getStats(CachePreheatService.SETMEAL_CACHE));
        stats.put("categoryCache", cacheHelper.getStats(CachePreheatService.CATEGORY_CACHE));
        
        return Result.success(stats);
    }

    /**
     * 手动触发缓存预热
     */
    @PostMapping("/preheat")
    @ApiOperation("手动触发缓存预热")
    public Result<String> manualPreheat() {
        log.info("手动触发缓存预热");
        cachePreheatService.manualPreheat();
        return Result.success("缓存预热已触发");
    }

    /**
     * 清空指定缓存
     */
    @DeleteMapping("/clear/{cacheName}")
    @ApiOperation("清空指定缓存")
    public Result<String> clearCache(@PathVariable String cacheName) {
        log.info("清空缓存: {}", cacheName);
        cacheSyncService.clear(cacheName);
        return Result.success("缓存已清空: " + cacheName);
    }

    /**
     * 删除指定缓存key
     */
    @DeleteMapping("/evict/{cacheName}")
    @ApiOperation("删除指定缓存key")
    public Result<String> evictCache(@PathVariable String cacheName, @RequestParam String key) {
        log.info("删除缓存 - name: {}, key: {}", cacheName, key);
        cacheSyncService.evict(cacheName, key);
        return Result.success("缓存已删除: " + cacheName + ":" + key);
    }

    /**
     * 获取缓存值
     */
    @GetMapping("/get/{cacheName}")
    @ApiOperation("获取缓存值")
    public Result<Object> getCache(@PathVariable String cacheName, @RequestParam String key) {
        Object value = cacheHelper.get(cacheName, key);
        return Result.success(value);
    }

    /**
     * 获取所有缓存名称
     */
    @GetMapping("/names")
    @ApiOperation("获取所有缓存名称")
    public Result<Iterable<String>> getCacheNames() {
        return Result.success(cacheManager.getCacheNames());
    }
}
