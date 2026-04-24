package com.mall.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    /** 用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为 6-20 位")
    private String password;
}
