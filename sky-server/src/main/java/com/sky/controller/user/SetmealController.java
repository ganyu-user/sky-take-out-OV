package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端-套餐浏览接口
 * 缓存实现在Service层，避免Controller层缓存Result包装类导致的序列化问题
 */
@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类id查询套餐
     * 缓存逻辑在SetmealService.listByCategoryId方法中实现
     *
     * @param categoryId 分类ID
     * @return 套餐列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<com.sky.entity.Setmeal>> list(Long categoryId) {
        List<com.sky.entity.Setmeal> list = setmealService.listByCategoryId(categoryId);
        return Result.success(list);
    }

    /**
     * 根据套餐id查询包含的菜品列表
     * 此接口不缓存（数据可能经常变动）
     *
     * @param id 套餐ID
     * @return 菜品列表
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }
}
