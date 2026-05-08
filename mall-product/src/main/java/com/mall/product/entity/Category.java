package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类实体
 * 支持多级分类（通过 parentId 构建树形结构）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    /** 分类名称 */
    private String name;

    /**
     * 父分类 ID
     * 顶级分类的 parentId = 0
     */
    private Long parentId;

    /**
     * 分类层级：1=一级，2=二级，3=三级
     * 冗余字段，避免递归查询层级
     */
    private Integer level;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 分类图标 URL */
    private String icon;
}
