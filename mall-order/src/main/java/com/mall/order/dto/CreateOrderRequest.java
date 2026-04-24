package com.mall.order.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求 DTO
 */
@Data
public class CreateOrderRequest {

    /** 订单商品列表（至少一个） */
    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    /** 收货地址 JSON */
    @NotNull(message = "收货地址不能为空")
    private String address;

    /** 备注 */
    private String remark;

    /**
     * 订单商品 DTO
     */
    @Data
    public static class OrderItemDTO {
        @NotNull
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        @NotNull
        private Integer quantity;
    }
}
