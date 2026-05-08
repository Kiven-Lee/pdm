package com.mall.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.order.entity.Order;
import com.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

/**
 * RocketMQ 事务消息监听器
 * <p>
 * 配合 OrderRocketMQProducer.sendOrderCreateTransaction() 使用。
 *
 * 职责：
 *   1. executeLocalTransaction：执行本地事务（将订单写入数据库）
 *   2. checkLocalTransaction：RocketMQ 回查本地事务状态
 *      （当 executeLocalTransaction 没有返回明确状态时触发）
 *
 * 注意：@RocketMQTransactionListener 的 rocketMQTemplateBeanName 需与
 * 发送时使用的 RocketMQTemplate Bean 名称一致。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    /**
     * 执行本地事务
     * <p>
     * 此方法在半消息发送成功后被调用。
     * 在这里执行真正的业务操作（写入订单到数据库）。
     * </p>
     *
     * @param message RocketMQ 消息（包含订单 JSON）
     * @param arg     发送时传入的参数（Order 对象）
     * @return 事务状态：
     *   COMMIT   = 提交，消费者可以消费此消息
     *   ROLLBACK = 回滚，消息被删除，消费者不可见
     *   UNKNOWN  = 未知，RocketMQ 稍后会回查
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            // arg 是发送时传入的 Order 对象
            Order order = (Order) arg;
            log.info("执行本地事务（写入订单）: orderNo={}", order.getOrderNo());

            // 将订单写入数据库
            int rows = orderMapper.insert(order);
            if (rows > 0) {
                // 订单写入成功，提交事务消息，物流服务可以消费
                log.info("本地事务执行成功，提交消息: orderNo={}", order.getOrderNo());
                return RocketMQLocalTransactionState.COMMIT;
            } else {
                // 写入失败，回滚消息
                log.warn("本地事务执行失败，回滚消息: orderNo={}", order.getOrderNo());
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        } catch (Exception e) {
            // 发生异常，回滚消息
            log.error("本地事务执行异常，回滚消息", e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 回查本地事务状态
     * <p>
     * 当 executeLocalTransaction 返回 UNKNOWN，或者没有响应时，
     * RocketMQ Broker 会定期（默认 60 秒）调用此方法回查事务状态。
     * 最多回查 15 次，超过后消息被丢弃。
     * </p>
     *
     * @param message RocketMQ 消息
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        try {
            // 从消息体中解析订单信息
            String payload = new String((byte[]) msg.getPayload());
            Order order = objectMapper.readValue(payload, Order.class);

            log.info("回查本地事务状态: orderNo={}", order.getOrderNo());

            // 查询数据库中是否存在此订单
            Order existOrder = orderMapper.selectById(order.getId());
            if (existOrder != null) {
                // 订单存在，说明本地事务已成功，提交消息
                return RocketMQLocalTransactionState.COMMIT;
            } else {
                // 订单不存在，说明本地事务失败，回滚消息
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        } catch (Exception e) {
            log.error("回查本地事务状态异常", e);
            // 回查异常时返回 UNKNOWN，等待下次回查
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }
}
