package com.sky.dto;

import com.sky.constant.MessageConstant;
import com.sky.entity.SetmealDish;
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
public class SetmealDTO implements Serializable {

    private Long id;

    @NotNull(message = MessageConstant.CATEGORY_OF_SETMEAL_NOT_NULL)
    private Long categoryId;

    @NotBlank(message = MessageConstant.SETMEAL_NAME_NOT_BLANK)
    @Size(min = 2, max = 20, message = MessageConstant.NAME_SIZE_ERROR)
    private String name;

    @NotNull(message = MessageConstant.SETMEAL_PRICE_NOT_NULL)
    @DecimalMin(value = "0.01", message = MessageConstant.PRICE_PATTERN_ERROR)
    private BigDecimal price;

    private Integer status;

    private String description;

    private String image;

    private List<SetmealDish> setmealDishes = new ArrayList<>();

}
