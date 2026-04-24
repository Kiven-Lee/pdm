package com.mall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类
 * EnableScheduling：开启 Spring 定时任务（订单超时取消）
 * EnableFeignClients：开启 Feign 客户端（调用商品服务）
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
