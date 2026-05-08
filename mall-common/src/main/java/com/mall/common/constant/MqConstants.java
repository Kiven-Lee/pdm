package com.mall.common.constant;

/**
 * MQ Topic/Tag 常量
 * <p>
 * 统一管理 RocketMQ 和 Kafka 的 Topic、Tag、Group 名称，
 * 避免生产者和消费者之间的字符串不一致问题。
 * </p>
 */
public class MqConstants {

    private MqConstants() {}

    // ===== RocketMQ：订单相关 =====
    /** 订单创建事务消息 Topic */
    public static final String TOPIC_ORDER_CREATE = "order-create-topic";
    /** 订单支付成功消息 Topic */
    public static final String TOPIC_ORDER_PAID = "order-paid-topic";
    /** 订单取消消息 Topic */
    public static final String TOPIC_ORDER_CANCEL = "order-cancel-topic";

    /** 物流服务消费组 */
    public static final String GROUP_LOGISTICS = "logistics-consumer-group";
    /** 订单服务消费组 */
    public static final String GROUP_ORDER = "order-consumer-group";

    // ===== Kafka：日志/流量相关 =====
    /** 商品浏览日志 Topic */
    public static final String KAFKA_TOPIC_PRODUCT_VIEW = "product-view-log";
    /** 物流操作日志 Topic */
    public static final String KAFKA_TOPIC_LOGISTICS_LOG = "logistics-operation-log";
    /** 订单操作日志 Topic */
    public static final String KAFKA_TOPIC_ORDER_LOG = "order-operation-log";
}
