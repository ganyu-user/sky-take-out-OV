package com.sky.controller.admin;

import com.sky.cache.CachePreheatService;
import com.sky.cache.CacheSyncService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "dish(菜品相关接口)")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CacheSyncService cacheSyncService;

    @Autowired
    private CachePreheatService cachePreheatService;

    /**
     * 新增菜品
     * 清除该分类的菜品缓存
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("save(新增菜品)")
    @CacheEvict(cacheNames = CachePreheatService.DISH_CACHE, key = "#dishDTO.categoryId")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 重新预热该分类的菜品缓存
        cachePreheatService.preheatDishByCategory(dishDTO.getCategoryId());

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("page(菜品分页查询)")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询:{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询菜品以及口味
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("getById(根据id查询菜品以及口味)")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品及其口味：{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId,String name){
        log.info("根据分类id查询菜品：{},{}",categoryId,name);
        List<Dish> dishList = dishService.list(categoryId, name);
        return Result.success(dishList);
    }

    /**
     * 批量删除商品
     * 清除所有菜品缓存
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除商品")
    @CacheEvict(cacheNames = CachePreheatService.DISH_CACHE, allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        dishService.deleteBatch(ids);

        // 清除所有菜品缓存
        cacheSyncService.clear(CachePreheatService.DISH_CACHE);

        return Result.success();
    }

    /**
     * 修改菜品
     * 清除所有菜品缓存
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("update(修改菜品)")
    @CacheEvict(cacheNames = CachePreheatService.DISH_CACHE, allEntries = true)
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        // 清除所有菜品缓存
        cacheSyncService.clear(CachePreheatService.DISH_CACHE);

        // 重新预热该分类的菜品缓存
        cachePreheatService.preheatDishByCategory(dishDTO.getCategoryId());

        return Result.success();
    }

    /**
     * 菜品起售或停售
     * 清除所有菜品缓存
     * @param status
     * @param id
     * @return
     */
    @ApiOperation("startOrStop(菜品起售和停售)")
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = CachePreheatService.DISH_CACHE, allEntries = true)
    public Result<String> startOrStop(@PathVariable("status") Integer status,Long id){
        log.info("修改商品售停状态：{},{}",status,id);
        dishService.startOrStop(status,id);

        // 清除所有菜品缓存
        cacheSyncService.clear(CachePreheatService.DISH_CACHE);

        return Result.success();
    }
}
