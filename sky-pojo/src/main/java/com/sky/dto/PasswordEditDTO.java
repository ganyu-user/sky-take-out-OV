package com.sky.dto;

import com.sky.constant.MessageConstant;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class PasswordEditDTO implements Serializable {

    private Long empId;

    @NotBlank(message = MessageConstant.OLDPASSWORD_NOT_BLANK)
    private String oldPassword;

    @NotBlank(message = MessageConstant.NEWPASSWORD_NOT_BLANK)
    @Size(min = 6, max = 20, message = MessageConstant.PASSWORD_SIZE_ERROR)
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = MessageConstant.PASSWORD_PATTERN_ERROR)
    private String newPassword;

}
