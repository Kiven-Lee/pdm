package com.mall.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    /** 用户名，3-20 位字母数字 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为 3-20 位")
    private String username;

    /** 密码，6-20 位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为 6-20 位")
    private String password;

    /** 手机号（可选） */
    private String phone;

    /** 邮箱（可选） */
    private String email;
}
