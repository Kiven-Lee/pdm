package com.mall.auth.controller;

import com.mall.auth.dto.LoginRequest;
import com.mall.auth.dto.RegisterRequest;
import com.mall.auth.dto.TokenResponse;
import com.mall.auth.service.AuthService;
import com.mall.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证控制器
 * <p>
 * 提供登录、注册、退出接口。
 * 这些接口在网关白名单中，不需要 JWT 验证。
 * </p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * POST /auth/register
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    /**
     * 用户登录
     * POST /auth/login
     *
     * @return 包含 accessToken、refreshToken 的响应
     */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return Result.success(tokenResponse);
    }

    /**
     * 退出登录
     * POST /auth/logout
     * 需要在请求头中携带 X-User-Id（由网关注入）
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId) {
        authService.logout(userId);
        return Result.success();
    }
}
