package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordEditDTO implements Serializable {

    //  该字段不该存在，此处前端不应直接传进用户ID
    private Long empId;
    // 旧密码
    private String oldPassword;
    // 新密码
    private String newPassword;

}
