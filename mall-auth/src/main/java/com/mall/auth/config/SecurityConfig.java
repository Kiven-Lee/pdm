package com.mall.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * <p>
 * 认证服务本身不做 JWT 校验（JWT 校验在网关层完成），
 * 这里主要配置：
 *   1. 禁用 CSRF（前后端分离项目不需要）
 *   2. 禁用 Session（使用无状态 JWT）
 *   3. 放行所有请求（鉴权由网关统一处理）
 *   4. 提供 BCrypt 密码编码器 Bean
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全过滤链配置
     * Spring Security 5.7+ 推荐使用 SecurityFilterChain Bean 替代继承 WebSecurityConfigurerAdapter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF：前后端分离项目使用 JWT，不需要 CSRF 保护
                .csrf().disable()
                // 禁用 CORS（跨域由网关统一处理）
                .cors().disable()
                // 禁用 Session：使用无状态 JWT，不在服务端存储会话
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 放行所有请求：认证服务的接口鉴权由网关 JWT 过滤器处理
                .authorizeRequests()
                    .anyRequest().permitAll();

        return http.build();
    }

    /**
     * BCrypt 密码编码器
     * <p>
     * BCrypt 特点：
     *   - 自动加盐（每次加密结果不同）
     *   - 计算成本可调（默认 strength=10，越大越慢越安全）
     *   - 不可逆，只能通过 matches() 验证
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
