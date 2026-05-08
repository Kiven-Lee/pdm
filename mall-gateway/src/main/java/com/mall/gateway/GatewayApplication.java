package com.mall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类
 * <p>
 * Spring Cloud Gateway 基于 WebFlux，启动时会自动排除 DataSource 自动配置。
 * 路由规则在 application.yml 中配置，JWT 鉴权由 JwtAuthFilter 全局过滤器处理。
 * </p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
