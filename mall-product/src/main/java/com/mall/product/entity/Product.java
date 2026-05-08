package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品实体
 * 对应数据库表 product
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品价格（使用 BigDecimal 避免浮点精度问题） */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 所属分类 ID */
    private Long categoryId;

    /** 商品主图 URL */
    private String mainImage;

    /** 商品图片列表（JSON 格式存储多张图片 URL） */
    private String images;

    /**
     * 商品状态：0=下架，1=上架
     * 下架商品不在前台展示，也不能加入购物车
     */
    private Integer status;

    /** 销量（冗余字段，避免频繁 JOIN order_item 统计） */
    private Integer sales;
}
