package com.mall.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqConstants;
import com.mall.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 订单 RocketMQ 生产者
 * <p>
 * 使用 RocketMQ 事务消息保证订单创建与库存扣减的最终一致性。
 *
 * 事务消息流程：
 *   1. 发送半消息（Half Message）到 RocketMQ，此时消费者不可见
 *   2. 执行本地事务（创建订单记录）
 *   3. 本地事务成功 → 提交消息（消费者可见）
 *      本地事务失败 → 回滚消息（消费者不可见，消息被删除）
 *   4. 如果步骤 3 没有响应，RocketMQ 会回查本地事务状态（OrderTransactionListener）
 *
 * 这样即使在步骤 2 和 3 之间发生宕机，也能通过回查保证最终一致性。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRocketMQProducer {

    /** RocketMQ Spring 模板，封装了 RocketMQ Producer API */
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送订单创建事务消息
     * <p>
     * 事务消息的 destination 格式：{topic}:{tag}
     * tag 用于消费者过滤，只消费感兴趣的消息
     * </p>
     *
     * @param order 订单对象（作为消息体）
     * @return 发送结果
     */
    public TransactionSendResult sendOrderCreateTransaction(Order order) {
        try {
            // 将订单对象序列化为 JSON 字符串作为消息体
            String orderJson = objectMapper.writeValueAsString(order);

            // 构建消息，设置消息 key 为订单编号（便于消息追踪和排查问题）
            Message<String> message = MessageBuilder
                    .withPayload(orderJson)
                    .setHeader("KEYS", order.getOrderNo())  // 消息 key，用于消息查询
                    .build();

            // 发送事务消息
            // destination = "topic:tag" 格式
            // arg = order（传递给 OrderTransactionListener 的本地事务参数）
            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
                    MqConstants.TOPIC_ORDER_CREATE,
                    message,
                    order  // 传递给事务监听器的参数
            );

            log.info("订单事务消息发送: orderNo={}, status={}",
                    order.getOrderNo(), result.getSendStatus());
            return result;

        } catch (Exception e) {
            log.error("订单事务消息发送失败: orderNo={}", order.getOrderNo(), e);
            throw new RuntimeException("订单消息发送失败", e);
        }
    }

    /**
     * 发送订单支付成功消息（普通消息，非事务）
     * 物流服务消费此消息后创建物流单
     *
     * @param order 已支付的订单
     */
    public void sendOrderPaidMessage(Order order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            // 同步发送，等待 Broker 确认
            SendResult result = rocketMQTemplate.syncSend(
                    MqConstants.TOPIC_ORDER_PAID, orderJson);

            if (result.getSendStatus() == SendStatus.SEND_OK) {
                log.info("订单支付消息发送成功: orderNo={}", order.getOrderNo());
            } else {
                log.warn("订单支付消息发送状态异常: orderNo={}, status={}",
                        order.getOrderNo(), result.getSendStatus());
            }
        } catch (Exception e) {
            log.error("订单支付消息发送失败: orderNo={}", order.getOrderNo(), e);
        }
    }
}
