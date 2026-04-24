package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.constant.RedisKeyConstants;
import com.mall.common.exception.BusinessException;
import com.mall.common.result.ResultCode;
import com.mall.order.dto.CreateOrderRequest;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mq.OrderRocketMQProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务
 * <p>
 * 核心功能：
 *   1. 创建订单（RocketMQ 事务消息保证最终一致性）
 *   2. 订单状态机流转（支付、发货、完成、取消）
 *   3. 超时未支付订单自动取消（配合定时任务）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderRocketMQProducer rocketMQProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 订单支付超时时间（分钟） */
    @Value("${order.pay-timeout-minutes:30}")
    private int payTimeoutMinutes;

    /**
     * 创建订单
     * <p>
     * 使用 RocketMQ 事务消息流程：
     *   1. 构建订单对象（不写数据库）
     *   2. 发送半消息到 RocketMQ
     *   3. OrderTransactionListener.executeLocalTransaction 写入数据库
     *   4. 写入成功 → 提交消息 → 物流服务消费
     *   5. 同时将订单 ID 写入 Redis ZSet，用于超时取消
     * </p>
     *
     * @param userId  下单用户 ID
     * @param request 创建订单请求
     * @return 订单编号
     */
    public String createOrder(Long userId, CreateOrderRequest request) {
        // 1. 生成订单编号（年月日时分秒 + 6位随机数）
        String orderNo = generateOrderNo();

        // 2. 计算订单总金额
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 构建订单实体（此时不写数据库，由事务消息监听器写入）
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 初始状态：待支付
        order.setAddress(request.getAddress());
        order.setRemark(request.getRemark());

        // 4. 发送 RocketMQ 事务消息
        // 事务消息会在 OrderTransactionListener 中执行本地事务（写入数据库）
        TransactionSendResult sendResult = rocketMQProducer.sendOrderCreateTransaction(order);

        // 5. 检查事务消息发送结果
        if (sendResult == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "订单创建失败，请重试");
        }

        // 6. 将订单加入 Redis ZSet，score = 超时时间戳，用于定时任务扫描超时订单
        // ZSet 按 score 排序，定时任务只需扫描 score <= 当前时间的订单
        long expireTimestamp = System.currentTimeMillis() + (long) payTimeoutMinutes * 60 * 1000;
        redisTemplate.opsForZSet().add(
                RedisKeyConstants.ORDER_TIMEOUT_ZSET,
                orderNo,
                expireTimestamp
        );

        log.info("订单创建成功: userId={}, orderNo={}, amount={}", userId, orderNo, totalAmount);
        return orderNo;
    }

    /**
     * 保存订单明细（由事务监听器在本地事务中调用）
     *
     * @param orderId 订单 ID
     * @param request 创建订单请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrderItems(Long orderId, CreateOrderRequest request) {
        List<OrderItem> items = request.getItems().stream().map(dto -> {
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(dto.getProductId());
            item.setProductName(dto.getProductName());
            item.setProductImage(dto.getProductImage());
            item.setPrice(dto.getPrice());
            item.setQuantity(dto.getQuantity());
            item.setTotalPrice(dto.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
            return item;
        }).collect(Collectors.toList());

        // 批量插入订单明细
        items.forEach(orderItemMapper::insert);
    }

    /**
     * 支付订单（模拟支付回调）
     * 状态流转：待支付(0) → 已支付(1)
     *
     * @param orderNo 订单编号
     * @param userId  用户 ID（校验订单归属）
     */
    public void payOrder(String orderNo, Long userId) {
        Order order = getOrderByNo(orderNo, userId);

        // 校验订单状态：只有待支付状态才能支付
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 更新订单状态（使用带状态校验的 SQL，防止并发问题）
        int rows = orderMapper.updateStatus(order.getId(), 0, 1, LocalDateTime.now());
        if (rows == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 从超时 ZSet 中移除（已支付，不再需要超时取消）
        redisTemplate.opsForZSet().remove(RedisKeyConstants.ORDER_TIMEOUT_ZSET, orderNo);

        // 发送支付成功消息，物流服务消费后创建物流单
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        rocketMQProducer.sendOrderPaidMessage(order);

        log.info("订单支付成功: orderNo={}", orderNo);
    }

    /**
     * 取消订单
     * 状态流转：待支付(0) → 已取消(4)
     */
    public void cancelOrder(String orderNo, Long userId) {
        Order order = getOrderByNo(orderNo, userId);

        // 只有待支付状态的订单可以取消
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "只有待支付订单可以取消");
        }

        int rows = orderMapper.updateStatus(order.getId(), 0, 4, LocalDateTime.now());
        if (rows == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 从超时 ZSet 中移除
        redisTemplate.opsForZSet().remove(RedisKeyConstants.ORDER_TIMEOUT_ZSET, orderNo);
        log.info("订单取消成功: orderNo={}", orderNo);
    }

    /**
     * 查询用户订单列表
     */
    public List<Order> listUserOrders(Long userId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );
    }

    /**
     * 查询订单明细
     */
    public List<OrderItem> listOrderItems(Long orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
    }

    /**
     * 根据订单编号查询订单（校验归属）
     */
    private Order getOrderByNo(String orderNo, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        // 校验订单归属，防止越权操作
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return order;
    }

    /**
     * 生成订单编号
     * 格式：yyyyMMddHHmmss + 6位随机数
     * 示例：20240101120000123456
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(900000) + 100000; // 6位随机数
        return timestamp + random;
    }
}
