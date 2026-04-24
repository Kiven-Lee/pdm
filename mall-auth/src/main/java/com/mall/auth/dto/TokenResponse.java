package com.mall.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO
 * 包含 accessToken 和 refreshToken
 */
@Data
@AllArgsConstructor
public class TokenResponse {

    /** 访问令牌，有效期较短（2小时），用于接口鉴权 */
    private String accessToken;

    /** 刷新令牌，有效期较长（7天），用于无感刷新 accessToken */
    private String refreshToken;

    /** accessToken 过期时间（毫秒时间戳） */
    private Long expireAt;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;
}
