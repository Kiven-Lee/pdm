package com.mall.gateway.filter;

import com.mall.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 全局鉴权过滤器
 * <p>
 * 网关层统一拦截所有请求，校验 JWT Token：
 *   1. 白名单路径直接放行（登录、注册、商品列表等）
 *   2. 非白名单路径必须携带有效 Token
 *   3. Token 有效则将 userId/username 写入请求头，传递给下游服务
 * </p>
 *
 * 注意：Gateway 基于 WebFlux（响应式），不能使用 Servlet API，
 * 需使用 ServerHttpRequest/ServerHttpResponse。
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /** JWT 签名密钥，从配置文件读取 */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** 白名单路径列表，从配置文件读取 */
    @Value("${jwt.white-list}")
    private List<String> whiteList;

    /** Ant 风格路径匹配器，支持 ** 和 ? 通配符 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 过滤器核心逻辑
     *
     * @param exchange 包含请求和响应的上下文
     * @param chain    过滤器链，调用 chain.filter() 继续执行后续过滤器
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 检查是否在白名单中，白名单路径直接放行
        if (isWhiteListed(path)) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 从请求头中获取 Authorization: Bearer <token>
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少 Authorization 头: {}", path);
            return writeUnauthorizedResponse(exchange, "未登录或登录已过期");
        }

        // 3. 提取 Token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(7);

        // 4. 解析并校验 Token
        Long userId = JwtUtil.getUserId(token, jwtSecret);
        String username = JwtUtil.getUsername(token, jwtSecret);
        if (userId == null) {
            log.warn("JWT Token 无效或已过期: {}", path);
            return writeUnauthorizedResponse(exchange, "Token 无效或已过期");
        }

        // 5. Token 有效，将用户信息写入请求头，传递给下游微服务
        // 下游服务通过读取这两个请求头获取当前用户信息，无需再次解析 Token
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username)
                .build();

        log.debug("JWT 鉴权通过: userId={}, path={}", userId, path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 过滤器执行顺序，数值越小优先级越高
     * 设为 -100 确保 JWT 过滤器在所有业务过滤器之前执行
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 判断请求路径是否在白名单中
     * 使用 AntPathMatcher 支持通配符匹配，如 /api/product/detail/**
     */
    private boolean isWhiteListed(String path) {
        return whiteList.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 向客户端写入 401 未授权响应
     * WebFlux 中响应是响应式的，需要返回 Mono<Void>
     *
     * @param message 错误提示信息
     */
    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构造 JSON 响应体
        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
