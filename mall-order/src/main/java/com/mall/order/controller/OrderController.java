package com.mall.order.controller;

import com.mall.common.result.Result;
import com.mall.order.dto.CreateOrderRequest;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     * POST /order/create
     */
    @PostMapping("/create")
    public Result<String> create(@RequestHeader("X-User-Id") Long userId,
                                 @Valid @RequestBody CreateOrderRequest request) {
        String orderNo = orderService.createOrder(userId, request);
        return Result.success(orderNo);
    }

    /**
     * 支付订单（模拟）
     * POST /order/pay/{orderNo}
     */
    @PostMapping("/pay/{orderNo}")
    public Result<Void> pay(@RequestHeader("X-User-Id") Long userId,
                            @PathVariable String orderNo) {
        orderService.payOrder(orderNo, userId);
        return Result.success();
    }

    /**
     * 取消订单
     * POST /order/cancel/{orderNo}
     */
    @PostMapping("/cancel/{orderNo}")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId,
                               @PathVariable String orderNo) {
        orderService.cancelOrder(orderNo, userId);
        return Result.success();
    }

    /**
     * 查询用户订单列表
     * GET /order/list
     */
    @GetMapping("/list")
    public Result<List<Order>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.listUserOrders(userId));
    }

    /**
     * 查询订单明细
     * GET /order/items/{orderId}
     */
    @GetMapping("/items/{orderId}")
    public Result<List<OrderItem>> items(@PathVariable Long orderId) {
        return Result.success(orderService.listOrderItems(orderId));
    }
}
