package com.mall.logistics.controller;

import com.mall.common.result.Result;
import com.mall.logistics.entity.Logistics;
import com.mall.logistics.entity.LogisticsTrack;
import com.mall.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物流控制器
 */
@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    /**
     * 根据订单 ID 查询物流信息
     * GET /logistics/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public Result<Logistics> getByOrderId(@PathVariable Long orderId) {
        return Result.success(logisticsService.getByOrderId(orderId));
    }

    /**
     * 根据快递单号查询物流轨迹
     * GET /logistics/track/{trackingNo}
     */
    @GetMapping("/track/{trackingNo}")
    public Result<List<LogisticsTrack>> getTrack(@PathVariable String trackingNo) {
        return Result.success(logisticsService.getTracksByTrackingNo(trackingNo));
    }
}
