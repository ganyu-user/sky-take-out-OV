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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "dish(菜品相关接口)")
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
    @PostMapping
    @ApiOperation("save(新增菜品)")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //  清理redis旧缓存，更新信息
        String key="dish_"+dishDTO.getCategoryId();
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
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除商品")
    public Result delete(@RequestParam List<Long> ids){
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
    @ApiOperation("update(修改菜品)")
    public Result update(DishDTO dishDTO){
        log.info("修改菜品：{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);
        clearCache("dish_*");
        return Result.success();
    }

    /**
     * 菜品起售或停售
     * @param status
     * @param id
     * @return
     */
    @ApiOperation("startOrStop(菜品起售和停售)")
    @PostMapping("/status/{status}")
    public Result<String> startOrStop(@PathVariable("status") Integer status,Long id){
        log.info("修改商品售停状态：{},{}",status,id);
        dishService.startOrStop(status,id);
        clearCache("dish_*");
        return Result.success();
    }
}
