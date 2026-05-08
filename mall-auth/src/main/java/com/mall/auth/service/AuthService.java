package com.mall.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.dto.LoginRequest;
import com.mall.auth.dto.RegisterRequest;
import com.mall.auth.dto.TokenResponse;
import com.mall.auth.entity.User;
import com.mall.auth.mapper.UserMapper;
import com.mall.common.constant.RedisKeyConstants;
import com.mall.common.exception.BusinessException;
import com.mall.common.result.ResultCode;
import com.mall.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 * <p>
 * 负责用户注册、登录、Token 刷新、退出登录等核心认证逻辑。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    /** JWT 签名密钥 */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** accessToken 过期时间（毫秒） */
    @Value("${jwt.expire}")
    private long jwtExpire;

    /** refreshToken 过期时间（毫秒） */
    @Value("${jwt.refresh-expire}")
    private long refreshExpire;

    /**
     * 用户注册
     * <p>
     * 流程：校验用户名唯一 → BCrypt 加密密码 → 写入数据库
     * </p>
     *
     * @param request 注册请求（用户名、密码等）
     */
    public void register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }

        // 2. 构建用户实体，密码使用 BCrypt 加密（不可逆哈希）
        User user = new User();
        user.setUsername(request.getUsername());
        // BCrypt 会自动加盐，每次加密结果不同，但 matches() 可以正确验证
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(0); // 默认正常状态

        // 3. 写入数据库（createTime/updateTime 由 MybatisPlusMetaHandler 自动填充）
        userMapper.insert(user);
        log.info("用户注册成功: username={}", request.getUsername());
    }

    /**
     * 用户登录
     * <p>
     * 流程：查询用户 → 校验密码 → 生成 accessToken + refreshToken → 存储 refreshToken 到 Redis
     * </p>
     *
     * @param request 登录请求（用户名、密码）
     * @return TokenResponse 包含双 Token 和用户信息
     */
    public TokenResponse login(LoginRequest request) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );

        // 2. 用户不存在或密码错误（统一返回相同错误，防止用户名枚举攻击）
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        // 3. 检查用户状态
        if (user.getStatus() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        // 4. 生成 accessToken（有效期短，用于接口鉴权）
        String accessToken = JwtUtil.generateToken(
                user.getId(), user.getUsername(), null, jwtSecret, jwtExpire);

        // 5. 生成 refreshToken（有效期长，用于无感刷新）
        String refreshToken = JwtUtil.generateToken(
                user.getId(), user.getUsername(), null, jwtSecret, refreshExpire);

        // 6. 将 refreshToken 存入 Redis，key = auth:refresh:{userId}
        // 存储目的：退出登录时可以删除此 key，使 refreshToken 失效
        String refreshKey = RedisKeyConstants.REFRESH_TOKEN + user.getId();
        redisTemplate.opsForValue().set(refreshKey, refreshToken,
                refreshExpire, TimeUnit.MILLISECONDS);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        // 7. 返回 Token 响应
        return new TokenResponse(
                accessToken,
                refreshToken,
                System.currentTimeMillis() + jwtExpire,
                user.getId(),
                user.getUsername()
        );
    }

    /**
     * 退出登录
     * <p>
     * 将 accessToken 加入黑名单（Redis），并删除 refreshToken。
     * 网关的 JWT 过滤器需要检查黑名单（此处简化：直接删除 refreshToken）。
     * </p>
     *
     * @param userId 当前用户 ID
     */
    public void logout(Long userId) {
        // 删除 Redis 中的 refreshToken，使其立即失效
        String refreshKey = RedisKeyConstants.REFRESH_TOKEN + userId;
        redisTemplate.delete(refreshKey);
        log.info("用户退出登录: userId={}", userId);
    }
}
