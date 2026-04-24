package com.mall.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物流单实体
 * 对应数据库表 logistics
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics")
public class Logistics extends BaseEntity {

    /** 关联的订单 ID */
    private Long orderId;

    /** 关联的订单编号 */
    private String orderNo;

    /** 物流公司名称（如：顺丰、圆通、中通） */
    private String company;

    /** 物流单号（快递单号） */
    private String trackingNo;

    /**
     * 物流状态：
     *   0 = 待揽收
     *   1 = 运输中
     *   2 = 派送中
     *   3 = 已签收
     *   4 = 异常
     */
    private Integer status;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人手机号 */
    private String receiverPhone;

    /** 收件地址 */
    private String receiverAddress;
}
