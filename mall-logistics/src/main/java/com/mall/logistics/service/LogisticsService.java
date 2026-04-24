package com.mall.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.logistics.entity.Logistics;
import com.mall.logistics.entity.LogisticsTrack;
import com.mall.logistics.mapper.LogisticsMapper;
import com.mall.logistics.mapper.LogisticsTrackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物流服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsMapper logisticsMapper;
    private final LogisticsTrackMapper logisticsTrackMapper;

    /**
     * 根据订单 ID 查询物流信息
     *
     * @param orderId 订单 ID
     * @return 物流信息（含轨迹）
     */
    public Logistics getByOrderId(Long orderId) {
        return logisticsMapper.selectOne(
                new LambdaQueryWrapper<Logistics>().eq(Logistics::getOrderId, orderId)
        );
    }

    /**
     * 根据快递单号查询物流轨迹
     *
     * @param trackingNo 快递单号
     * @return 物流轨迹列表（按时间升序）
     */
    public List<LogisticsTrack> getTracksByTrackingNo(String trackingNo) {
        // 先查物流单
        Logistics logistics = logisticsMapper.selectOne(
                new LambdaQueryWrapper<Logistics>().eq(Logistics::getTrackingNo, trackingNo)
        );
        if (logistics == null) {
            return List.of();
        }

        // 查询轨迹，按时间升序排列
        return logisticsTrackMapper.selectList(
                new LambdaQueryWrapper<LogisticsTrack>()
                        .eq(LogisticsTrack::getLogisticsId, logistics.getId())
                        .orderByAsc(LogisticsTrack::getTrackTime)
        );
    }
}
