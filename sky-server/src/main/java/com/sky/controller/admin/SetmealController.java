package com.sky.controller.admin;

import com.sky.cache.CachePreheatService;
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

/**
 * 管理端-套餐管理接口
 * 使用Spring Cache注解实现缓存一致性
 */
@RestController
@Slf4j
@Api(tags = "管理端-套餐相关接口")
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     * @CacheEvict: 清除该分类的套餐缓存
     *
     * @param setmealDTO 套餐信息
     * @return 操作结果
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(
            cacheNames = CachePreheatService.SETMEAL_CACHE,
            key = "#setmealDTO.categoryId"
    )
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐: {}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * 管理端查询不缓存
     *
     * @param setmealPageQueryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询: {}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除套餐
     * @CacheEvict: 清除所有套餐缓存
     *
     * @param ids 套餐ID列表
     * @return 操作结果
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    @CacheEvict(
            cacheNames = CachePreheatService.SETMEAL_CACHE,
            allEntries = true
    )
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除套餐: {}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐（回显）
     * 管理端查询不缓存
     *
     * @param id 套餐ID
     * @return 套餐详情
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐
     * @CacheEvict: 清除所有套餐缓存
     *
     * @param setmealDTO 套餐信息
     * @return 操作结果
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(
            cacheNames = CachePreheatService.SETMEAL_CACHE,
            allEntries = true
    )
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐: {}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐停售起售
     * @CacheEvict: 清除所有套餐缓存
     *
     * @param status 状态（0停售 1起售）
     * @param id 套餐ID
     * @return 操作结果
     */
    @PostMapping("/status/{status}")
    @ApiOperation("套餐停售起售")
    @CacheEvict(
            cacheNames = CachePreheatService.SETMEAL_CACHE,
            allEntries = true
    )
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("修改套餐状态: status={}, id={}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }
}
