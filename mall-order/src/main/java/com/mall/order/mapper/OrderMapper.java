package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 更新订单状态（带状态校验，防止状态回退）
     * 只有当前状态等于 fromStatus 时才允许更新为 toStatus
     *
     * @param orderId    订单 ID
     * @param fromStatus 期望的当前状态
     * @param toStatus   目标状态
     * @param updateTime 更新时间
     * @return 影响行数：1=成功，0=状态不匹配
     */
    @Update("UPDATE mall_order SET status = #{toStatus}, update_time = #{updateTime} " +
            "WHERE id = #{orderId} AND status = #{fromStatus} AND deleted = 0")
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus,
                     @Param("updateTime") LocalDateTime updateTime);
}
