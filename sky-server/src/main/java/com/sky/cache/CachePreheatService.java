package com.sky.cache;

import com.sky.constant.StatusConstant;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 缓存预热服务
 * 在系统启动时预加载热点数据到缓存中
 * 
 * 预热数据包括：
 * 1. 启用的分类列表
 * 2. 热门菜品（按分类）
 * 3. 启用的套餐列表
 */
@Slf4j
@Component
public class CachePreheatService implements ApplicationRunner {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    // 缓存名称常量
    public static final String DISH_CACHE = "dishCache";
    public static final String SETMEAL_CACHE = "setmealCache";
    public static final String CATEGORY_CACHE = "categoryCache";

    /**
     * 系统启动时执行缓存预热
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始缓存预热 ==========");
        
        try {
            // 预热分类缓存
            preheatCategoryCache();
            
            // 预热菜品缓存
            preheatDishCache();
            
            // 预热套餐缓存
            preheatSetmealCache();
            
            log.info("========== 缓存预热完成 ==========");
        } catch (Exception e) {
            log.error("缓存预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热分类缓存
     */
    private void preheatCategoryCache() {
        log.info("预热分类缓存...");
        
        // 查询所有启用的分类（type为null表示查询所有类型）
        List<Category> categoryList = categoryMapper.list(null);
        
        // 过滤出启用的分类
        if (categoryList != null && !categoryList.isEmpty()) {
            categoryList = categoryList.stream()
                    .filter(cat -> cat.getStatus() != null && cat.getStatus().equals(StatusConstant.ENABLE))
                    .collect(java.util.stream.Collectors.toList());

            if (!categoryList.isEmpty()) {
                Cache cache = cacheManager.getCache(CATEGORY_CACHE);
                if (cache != null) {
                    cache.put("all", categoryList);
                    log.info("分类缓存预热完成，共 {} 条数据", categoryList.size());
                }
            }
        }
    }

    /**
     * 预热菜品缓存
     * 按分类ID预加载启用的菜品
     */
    private void preheatDishCache() {
        log.info("预热菜品缓存...");
        
        // 查询所有启用的分类（type为null表示查询所有类型）
        List<Category> categoryList = categoryMapper.list(null);
        
        // 过滤出启用的分类
        if (categoryList != null && !categoryList.isEmpty()) {
            categoryList = categoryList.stream()
                    .filter(cat -> cat.getStatus() != null && cat.getStatus().equals(StatusConstant.ENABLE))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (categoryList == null || categoryList.isEmpty()) {
            log.warn("没有可用的分类，跳过菜品缓存预热");
            return;
        }
        
        Cache cache = cacheManager.getCache(DISH_CACHE);
        if (cache == null) {
            log.warn("菜品缓存未配置，跳过预热");
            return;
        }
        
        int count = 0;
        for (Category cat : categoryList) {
            try {
                // 查询该分类下的菜品
                Dish dish = new Dish();
                dish.setCategoryId(cat.getId());
                dish.setStatus(StatusConstant.ENABLE);
                
                List<DishVO> dishList = dishService.listWithFlavor(dish);
                
                if (dishList != null && !dishList.isEmpty()) {
                    // 使用分类ID作为缓存key
                    cache.put(String.valueOf(cat.getId()), dishList);
                    count += dishList.size();
                }
            } catch (Exception e) {
                log.warn("预热分类 {} 的菜品缓存失败: {}", cat.getId(), e.getMessage());
            }
        }
        
        log.info("菜品缓存预热完成，共 {} 条数据", count);
    }

    /**
     * 预热套餐缓存
     * 按分类ID预加载启用的套餐
     */
    private void preheatSetmealCache() {
        log.info("预热套餐缓存...");
        
        // 查询所有启用的分类（type为null表示查询所有类型）
        List<Category> categoryList = categoryMapper.list(null);
        
        // 过滤出启用的分类
        if (categoryList != null && !categoryList.isEmpty()) {
            categoryList = categoryList.stream()
                    .filter(cat -> cat.getStatus() != null && cat.getStatus().equals(StatusConstant.ENABLE))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (categoryList == null || categoryList.isEmpty()) {
            log.warn("没有可用的分类，跳过套餐缓存预热");
            return;
        }
        
        Cache cache = cacheManager.getCache(SETMEAL_CACHE);
        if (cache == null) {
            log.warn("套餐缓存未配置，跳过预热");
            return;
        }
        
        int count = 0;
        for (Category cat : categoryList) {
            try {
                // 查询该分类下的套餐
                Setmeal setmeal = new Setmeal();
                setmeal.setCategoryId(cat.getId());
                setmeal.setStatus(StatusConstant.ENABLE);
                
                List<Setmeal> setmealList = setmealService.list(setmeal);
                
                if (setmealList != null && !setmealList.isEmpty()) {
                    // 使用分类ID作为缓存key
                    cache.put(String.valueOf(cat.getId()), setmealList);
                    count += setmealList.size();
                }
            } catch (Exception e) {
                log.warn("预热分类 {} 的套餐缓存失败: {}", cat.getId(), e.getMessage());
            }
        }
        
        log.info("套餐缓存预热完成，共 {} 条数据", count);
    }

    /**
     * 手动触发缓存预热
     * 可用于定时任务或管理接口
     */
    public void manualPreheat() {
        log.info("手动触发缓存预热...");
        run(null);
    }

    /**
     * 预热指定分类的菜品缓存
     * 
     * @param categoryId 分类ID
     */
    public void preheatDishByCategory(Long categoryId) {
        log.info("预热分类 {} 的菜品缓存", categoryId);
        
        try {
            Cache cache = cacheManager.getCache(DISH_CACHE);
            if (cache == null) {
                return;
            }
            
            Dish dish = new Dish();
            dish.setCategoryId(categoryId);
            dish.setStatus(StatusConstant.ENABLE);
            
            List<DishVO> dishList = dishService.listWithFlavor(dish);
            
            if (dishList != null && !dishList.isEmpty()) {
                cache.put(String.valueOf(categoryId), dishList);
                log.info("分类 {} 的菜品缓存预热完成，共 {} 条数据", categoryId, dishList.size());
            }
        } catch (Exception e) {
            log.error("预热分类 {} 的菜品缓存失败: {}", categoryId, e.getMessage());
        }
    }

    /**
     * 预热指定分类的套餐缓存
     * 
     * @param categoryId 分类ID
     */
    public void preheatSetmealByCategory(Long categoryId) {
        log.info("预热分类 {} 的套餐缓存", categoryId);
        
        try {
            Cache cache = cacheManager.getCache(SETMEAL_CACHE);
            if (cache == null) {
                return;
            }
            
            Setmeal setmeal = new Setmeal();
            setmeal.setCategoryId(categoryId);
            setmeal.setStatus(StatusConstant.ENABLE);
            
            List<Setmeal> setmealList = setmealService.list(setmeal);
            
            if (setmealList != null && !setmealList.isEmpty()) {
                cache.put(String.valueOf(categoryId), setmealList);
                log.info("分类 {} 的套餐缓存预热完成，共 {} 条数据", categoryId, setmealList.size());
            }
        } catch (Exception e) {
            log.error("预热分类 {} 的套餐缓存失败: {}", categoryId, e.getMessage());
        }
    }
}
