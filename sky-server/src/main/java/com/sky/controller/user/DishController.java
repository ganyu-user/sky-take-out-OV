package com.sky.controller.user;

import com.sky.cache.CachePreheatService;
import com.sky.cache.CacheSyncService;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private CacheSyncService cacheSyncService;

    @Autowired
    private CachePreheatService cachePreheatService;

    /**
     * 根据分类id查询菜品
     * 使用多级缓存：Caffeine本地缓存 + Redis分布式缓存
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("查询菜品 - 分类ID: {}", categoryId);

        // 使用CacheHelper手动操作缓存
        List<DishVO> list = cacheSyncService.get(
                CachePreheatService.DISH_CACHE,
                String.valueOf(categoryId),
                List.class);

        if (list == null) {
            // 缓存未命中，查询数据库
            Dish dish = new Dish();
            dish.setCategoryId(categoryId);
            dish.setStatus(StatusConstant.ENABLE);
            list = dishService.listWithFlavor(dish);

            // 放入缓存
            cacheSyncService.put(
                    CachePreheatService.DISH_CACHE,
                    String.valueOf(categoryId),
                    list);
        }

        return Result.success(list);
    }
}
