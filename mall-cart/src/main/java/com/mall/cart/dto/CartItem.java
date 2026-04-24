package com.mall.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目 DTO
 * <p>
 * 购物车数据完全存储在 Redis Hash 中，不持久化到 MySQL。
 * Redis 结构：
 *   key   = cart:{userId}          （Hash 的 key）
 *   field = {productId}            （Hash 的 field，即商品 ID）
 *   value = CartItem JSON 字符串   （Hash 的 value）
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    private Long productId;

    /** 商品名称（冗余存储，避免每次查询商品服务） */
    private String productName;

    /** 商品主图 */
    private String mainImage;

    /** 加购时的单价（快照价格，下单时以此为准） */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 是否选中（结算时只计算选中的商品） */
    private Boolean checked;
}
