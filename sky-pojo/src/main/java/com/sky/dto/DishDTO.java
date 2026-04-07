package com.sky.dto;

import com.sky.constant.MessageConstant;
import com.sky.entity.DishFlavor;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {

    private Long id;

    @NotBlank(message =MessageConstant.DISH_NAME_NOT_BLANK)
    @Size(min = 2, max = 20, message = MessageConstant.NAME_SIZE_ERROR)
    private String name;

    @NotNull(message = MessageConstant.CATEGORY_OF_DISH_NOT_NULL)
    private Long categoryId;

    @NotNull(message = MessageConstant.DISH_PRICE_NOT_NULL)
    @DecimalMin(value = "0.01", message = MessageConstant.PRICE_ERROR)
    private BigDecimal price;

    private String image;

    private String description;

    private Integer status;

    private List<DishFlavor> flavors = new ArrayList<>();

}
