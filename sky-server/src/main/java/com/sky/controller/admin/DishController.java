package com.sky.controller.admin;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@Slf4j
@RestController
@Api(tags = "Dish(菜品相关接口)")
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 清理redis旧缓存，保持与数据库数据一致
     * @param pattern
     */
    private void clearCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @ApiOperation("save(新增菜品)")
    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //清理redis缓存数据
        String key = "dish_"+dishDTO.getCategoryId();
        clearCache(key);

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
        log.info("菜品分页查询：{}",dishPageQueryDTO);
        PageResult pageResult=dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("delete(菜单删除)")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除：{}",ids);
        dishService.deleteBatch(ids);

        //清理redis缓存数据,清理所有以dish_开头的key
        clearCache("dish_*");

        return Result.success();
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("update(修改菜品信息)")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //清理redis缓存数据,清理所有以dish_开头的key
        clearCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("getById(根据id查询菜品)")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品：{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return  Result.success(dishVO);
    }

    /**
     * 菜品起售或停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("startOrStop(菜品起售和停售)")
    public Result<String> startOrStop(@PathVariable("status") Integer status,Long id){
        log.info("菜品起售、停售：{},{}",status,id);
        dishService.startOrStop(status,id);

        //清理redis缓存数据,清理所有以dish_开头的key
        clearCache("dish_*");

        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("list(根据分类id查询菜品)")
    public Result<List<Dish>> list(Long categoryId,String name){
        List<Dish> list = dishService.list(categoryId,name);
        return Result.success(list);
    }
}
