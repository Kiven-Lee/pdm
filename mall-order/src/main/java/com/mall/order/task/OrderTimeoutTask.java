package com.mall.order.task;

import com.mall.common.constant.RedisKeyConstants;
import com.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 订单超时取消定时任务
 * <p>
 * 使用 Redis ZSet 实现延迟队列：
 *   - 创建订单时，将订单编号以超时时间戳为 score 写入 ZSet
 *   - 定时任务每分钟扫描 score <= 当前时间戳的订单（即已超时的订单）
 *   - 将超时订单状态更新为已取消
 *
 * 优点：
 *   - 不需要扫描全表，只扫描 ZSet 中的超时订单
 *   - 时间复杂度 O(log N + M)，N=ZSet 大小，M=超时订单数量
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderMapper orderMapper;

    /**
     * 每分钟执行一次，扫描并取消超时未支付订单
     * cron 表达式：0 * * * * ? = 每分钟的第 0 秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        long now = System.currentTimeMillis();

        // 从 ZSet 中获取 score <= 当前时间戳的所有订单编号
        // 即所有已超时的订单
        Set<Object> timeoutOrderNos = redisTemplate.opsForZSet()
                .rangeByScore(RedisKeyConstants.ORDER_TIMEOUT_ZSET, 0, now);

        if (timeoutOrderNos == null || timeoutOrderNos.isEmpty()) {
            return;
        }

        log.info("发现超时订单 {} 个，开始处理", timeoutOrderNos.size());

        for (Object orderNoObj : timeoutOrderNos) {
            String orderNo = orderNoObj.toString();
            try {
                // 查询订单，只取消待支付状态的订单
                // 使用 updateStatus 方法（带状态校验），防止取消已支付的订单
                int rows = orderMapper.updateStatus(
                        getOrderIdByNo(orderNo), // 需要先查出订单 ID
                        0,  // fromStatus = 待支付
                        4,  // toStatus = 已取消
                        LocalDateTime.now()
                );

                if (rows > 0) {
                    log.info("超时订单已取消: orderNo={}", orderNo);
                }

                // 无论是否取消成功，都从 ZSet 中移除（避免重复处理）
                redisTemplate.opsForZSet().remove(RedisKeyConstants.ORDER_TIMEOUT_ZSET, orderNo);

            } catch (Exception e) {
                log.error("处理超时订单失败: orderNo={}", orderNo, e);
                // 处理失败时不从 ZSet 移除，下次继续尝试
            }
        }
    }

    /**
     * 根据订单编号查询订单 ID
     * 注意：此处简化实现，实际应通过 orderNo 查询
     */
    private Long getOrderIdByNo(String orderNo) {
        com.mall.order.entity.Order order = orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.order.entity.Order>()
                        .eq(com.mall.order.entity.Order::getOrderNo, orderNo)
                        .select(com.mall.order.entity.Order::getId)
        );
        return order != null ? order.getId() : null;
    }
}
