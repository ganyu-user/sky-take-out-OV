package com.sky.controller.admin;

import com.sky.cache.CachePreheatService;
import com.sky.cache.CacheSyncService;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api(tags = "Setmeal(套餐相关接口)")
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CacheSyncService cacheSyncService;

    @Autowired
    private CachePreheatService cachePreheatService;

    /**
     * 新增套餐
     * 清除该分类的套餐缓存
     * @param setmealDTO
     * @return
     */
    @ApiOperation("save(新增套餐)")
    @PostMapping
    @CacheEvict(cacheNames = CachePreheatService.SETMEAL_CACHE, key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);

        // 重新预热该分类的套餐缓存
        cachePreheatService.preheatSetmealByCategory(setmealDTO.getCategoryId());

        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("page(分页查询)")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除套餐
     * 清除所有套餐缓存
     * @param ids
     * @return
     */
    @ApiOperation("delete(批量删除套餐)")
    @DeleteMapping
    @CacheEvict(cacheNames = CachePreheatService.SETMEAL_CACHE, allEntries = true)
    public Result delete(@RequestParam List<Long> ids) {
        setmealService.deleteBatch(ids);

        // 清除所有套餐缓存
        cacheSyncService.clear(CachePreheatService.SETMEAL_CACHE);

        return Result.success();
    }

    /**
     * 根据id查询套餐（回显）
     * @param id
     * @return
     */
    @ApiOperation("getById(根据id查询套餐)")
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        SetmealVO setmealVO=setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐
     * 清除所有套餐缓存
     * @param setmealDTO
     * @return
     */
    @ApiOperation("update(修改套餐)")
    @PutMapping
    @CacheEvict(cacheNames = CachePreheatService.SETMEAL_CACHE, allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        setmealService.update(setmealDTO);

        // 清除所有套餐缓存
        cacheSyncService.clear(CachePreheatService.SETMEAL_CACHE);

        // 重新预热该分类的套餐缓存
        cachePreheatService.preheatSetmealByCategory(setmealDTO.getCategoryId());

        return Result.success();
    }

    /**
     * 套餐停售起售
     * 清除所有套餐缓存
     * @param status
     * @param id
     * @return
     */
    @ApiOperation("startOrStop(套餐停售起售)")
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = CachePreheatService.SETMEAL_CACHE, allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        setmealService.startOrStop(status,id);

        // 清除所有套餐缓存
        cacheSyncService.clear(CachePreheatService.SETMEAL_CACHE);

        return Result.success();
    }
}
