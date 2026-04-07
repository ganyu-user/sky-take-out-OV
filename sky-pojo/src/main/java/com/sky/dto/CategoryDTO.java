package com.sky.dto;

import lombok.Data;
import com.sky.constant.MessageConstant;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {

    private Long id;

    private Integer type;

    @NotBlank(message = MessageConstant.CATEGORY_NAME_NOT_BLANK)
    @Size(min = 2, max = 20, message = MessageConstant.NAME_SIZE_ERROR)
    private String name;

    @NotNull(message = MessageConstant.SORT_NOT_NULL)
    @Digits(integer = 10, fraction = 0, message = MessageConstant.SORT_ERROR)
    private Integer sort;

}
