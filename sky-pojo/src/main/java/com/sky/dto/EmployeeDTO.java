package com.sky.dto;

import com.sky.constant.MessageConstant;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    @NotBlank(message = MessageConstant.USERNAME_NOT_BLANK)
    @Size(min = 3, max = 20, message = MessageConstant.USERNAME_SIZE_ERROR)
    private String username;

    @NotBlank(message = MessageConstant.NAME_NOT_BLANK)
    @Size(min = 2, max = 20, message = MessageConstant.NAME_SIZE_ERROR)
    private String name;

    @NotBlank(message = MessageConstant.PHONE_NOT_BLANK)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = MessageConstant.PHONE_SIZE_ERROR)
    private String phone;

    private String sex;

    @NotBlank(message = MessageConstant.ID_NUMBER_NOT_BLANK)
    @Pattern(regexp = "^\\d{17}[0-9Xx]$", message = MessageConstant.ID_NUMBER_SIZE_ERROR)
    private String idNumber;

}
