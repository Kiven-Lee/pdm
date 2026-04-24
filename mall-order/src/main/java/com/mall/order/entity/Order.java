package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * 对应数据库表 mall_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class Order extends BaseEntity {

    /** 订单编号（业务唯一标识，格式：年月日时分秒+随机数） */
    private String orderNo;

    /** 下单用户 ID */
    private Long userId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /**
     * 订单状态（状态机）：
     *   0 = 待支付
     *   1 = 已支付（待发货）
     *   2 = 已发货（待收货）
     *   3 = 已完成（已收货）
     *   4 = 已取消
     */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 完成时间（确认收货时间） */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 收货地址（JSON 格式，包含姓名、手机、省市区、详细地址） */
    private String address;

    /** 备注 */
    private String remark;
}
