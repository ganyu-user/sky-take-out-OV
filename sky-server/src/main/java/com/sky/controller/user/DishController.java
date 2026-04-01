package com.sky.controller.user;

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

/**
 * C端-菜品浏览接口
 * 缓存实现在Service层，避免Controller层缓存Result包装类导致的序列化问题
 */
@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 根据分类id查询菜品
     * 缓存逻辑在DishService.listWithFlavor方法中实现
     *
     * @param categoryId 分类ID
     * @return 菜品列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("查询菜品 - 分类ID: {}", categoryId);

        List<DishVO> list = dishService.listWithFlavor(categoryId);

        log.info("查询完成 - 分类ID: {}, 结果数量: {}", categoryId, list != null ? list.size() : 0);
        return Result.success(list);
    }
}
