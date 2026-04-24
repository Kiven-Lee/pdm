package com.mall.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * <p>
 * 封装 JWT 的生成、解析、校验逻辑。
 * 使用 HMAC-SHA256 对称签名算法，密钥长度需 >= 256 bit（32字节）。
 * </p>
 *
 * 使用示例：
 *   String token = JwtUtil.generateToken(userId, username, extraClaims, secret, expireMs);
 *   Claims claims = JwtUtil.parseToken(token, secret);
 */
@Slf4j
public class JwtUtil {

    // 私有构造，工具类不允许实例化
    private JwtUtil() {}

    /** JWT Payload 中存储用户 ID 的 key */
    public static final String CLAIM_USER_ID = "userId";
    /** JWT Payload 中存储用户名的 key */
    public static final String CLAIM_USERNAME = "username";

    /**
     * 生成 JWT Token
     *
     * @param userId    用户 ID（存入 Payload）
     * @param username  用户名（存入 Payload）
     * @param extraClaims 额外的自定义 Claim（可为 null）
     * @param secret    签名密钥（至少 32 字节）
     * @param expireMs  过期时间（毫秒）
     * @return 签名后的 JWT 字符串
     */
    public static String generateToken(Long userId, String username,
                                       Map<String, Object> extraClaims,
                                       String secret, long expireMs) {
        // 将字符串密钥转换为 HMAC-SHA256 所需的 Key 对象
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // 构建 JWT Builder
        var builder = Jwts.builder()
                // 设置主题（通常为用户标识）
                .setSubject(String.valueOf(userId))
                // 写入用户 ID 和用户名到 Payload
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                // 签发时间
                .setIssuedAt(new Date())
                // 过期时间 = 当前时间 + expireMs
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                // 使用 HS256 算法签名
                .signWith(key, SignatureAlgorithm.HS256);

        // 如果有额外 Claim，逐一添加
        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /**
     * 解析 JWT Token，返回 Claims（Payload 内容）
     *
     * @param token  JWT 字符串
     * @param secret 签名密钥
     * @return Claims 对象，解析失败返回 null
     */
    public static Claims parseToken(String token, String secret) {
        try {
            Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    // parseClaimsJws 会同时验证签名和过期时间
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            // Token 无效（签名错误、已过期、格式错误等）
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 Token 是否有效（签名正确且未过期）
     *
     * @param token  JWT 字符串
     * @param secret 签名密钥
     * @return true=有效，false=无效
     */
    public static boolean isValid(String token, String secret) {
        return parseToken(token, secret) != null;
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token  JWT 字符串
     * @param secret 签名密钥
     * @return 用户 ID，解析失败返回 null
     */
    public static Long getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        if (claims == null) {
            return null;
        }
        // Claim 中存储的是 Integer 类型，需转换为 Long
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return userId instanceof Long ? (Long) userId : null;
    }

    /**
     * 从 Token 中提取用户名
     */
    public static String getUsername(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims == null ? null : claims.get(CLAIM_USERNAME, String.class);
    }
}
