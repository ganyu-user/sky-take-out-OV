package com.sky.controller.admin;

import com.sky.cache.CachePreheatService;
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

/**
 * 管理端-菜品管理接口
 * 使用Spring Cache注解实现缓存一致性
 */
@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "管理端-菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品
     * @CacheEvict: 清除指定分类的缓存
     *
     * @param dishDTO 菜品信息
     * @return 操作结果
     */
    @PostMapping
    @ApiOperation("新增菜品")
    @CacheEvict(
            cacheNames = CachePreheatService.DISH_CACHE,
            key = "#dishDTO.categoryId"  // 清除该分类的缓存
    )
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * 管理端查询不缓存（数据量大，且管理端对实时性要求高）
     *
     * @param dishPageQueryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询: {}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询菜品以及口味
     * 管理端查询不缓存
     *
     * @param id 菜品ID
     * @return 菜品详情
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品以及口味")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品及其口味：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 根据分类id查询菜品
     * 管理端查询不缓存
     *
     * @param categoryId 分类ID
     * @param name 菜品名称（可选）
     * @return 菜品列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId, String name) {
        log.info("根据分类id查询菜品：{}, {}", categoryId, name);
        List<Dish> dishList = dishService.list(categoryId, name);
        return Result.success(dishList);
    }

    /**
     * 批量删除商品
     * @CacheEvict: 清除所有菜品缓存（因为不知道影响哪些分类）
     *
     * @param ids 菜品ID列表
     * @return 操作结果
     */
    @DeleteMapping
    @ApiOperation("批量删除商品")
    @CacheEvict(
            cacheNames = CachePreheatService.DISH_CACHE,
            allEntries = true  // 清除所有缓存
    )
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除商品: {}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改菜品
     * @CacheEvict: 清除所有菜品缓存
     *
     * @param dishDTO 菜品信息
     * @return 操作结果
     */
    @PutMapping
    @ApiOperation("修改菜品")
    @CacheEvict(
            cacheNames = CachePreheatService.DISH_CACHE,
            allEntries = true
    )
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品起售或停售
     * @CacheEvict: 清除所有菜品缓存
     *
     * @param status 状态（0停售 1起售）
     * @param id 菜品ID
     * @return 操作结果
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售和停售")
    @CacheEvict(
            cacheNames = CachePreheatService.DISH_CACHE,
            allEntries = true
    )
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id) {
        log.info("修改商品售停状态：{}, {}", status, id);
        dishService.startOrStop(status, id);
        return Result.success();
    }
}
