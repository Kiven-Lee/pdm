package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细实体
 * 对应数据库表 order_item
 * 一个订单可以包含多个商品（一对多关系）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {

    /** 所属订单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 商品名称（快照，防止商品信息变更影响历史订单） */
    private String productName;

    /** 商品主图（快照） */
    private String productImage;

    /** 下单时的单价（快照，防止价格变更影响历史订单） */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 = price * quantity */
    private BigDecimal totalPrice;
}
