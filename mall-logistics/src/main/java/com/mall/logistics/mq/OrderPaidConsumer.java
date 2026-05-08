package com.mall.logistics.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqConstants;
import com.mall.logistics.entity.Logistics;
import com.mall.logistics.entity.LogisticsTrack;
import com.mall.logistics.mapper.LogisticsMapper;
import com.mall.logistics.mapper.LogisticsTrackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 订单支付消息消费者（RocketMQ）
 * <p>
 * 消费订单服务发送的支付成功消息，创建物流单。
 *
 * @RocketMQMessageListener 参数说明：
 *   - topic：订阅的 Topic，与生产者发送的 Topic 一致
 *   - consumerGroup：消费者组，同一组内的消费者共同消费消息（负载均衡）
 *   - consumeMode：消费模式，CONCURRENTLY=并发消费（默认），ORDERLY=顺序消费
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.TOPIC_ORDER_PAID,
        consumerGroup = MqConstants.GROUP_LOGISTICS
)
public class OrderPaidConsumer implements RocketMQListener<String> {

    private final LogisticsMapper logisticsMapper;
    private final LogisticsTrackMapper logisticsTrackMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 消费订单支付消息
     * <p>
     * 消费逻辑：
     *   1. 解析订单信息
     *   2. 创建物流单（分配快递公司和单号）
     *   3. 创建初始物流轨迹（揽收节点）
     *   4. 发送物流操作日志到 Kafka
     * </p>
     *
     * @param message 消息内容（订单 JSON 字符串）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String message) {
        log.info("收到订单支付消息: {}", message);

        try {
            // 1. 解析订单 JSON
            Map<String, Object> orderMap = objectMapper.readValue(message, Map.class);
            Long orderId = Long.valueOf(orderMap.get("id").toString());
            String orderNo = orderMap.get("orderNo").toString();
            String address = orderMap.getOrDefault("address", "{}").toString();

            // 2. 检查是否已创建物流单（幂等处理，防止消息重复消费）
            Long existCount = logisticsMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Logistics>()
                            .eq(Logistics::getOrderId, orderId)
            );
            if (existCount > 0) {
                log.warn("物流单已存在，跳过重复消费: orderId={}", orderId);
                return;
            }

            // 3. 创建物流单（模拟分配快递公司和单号）
            Logistics logistics = new Logistics();
            logistics.setOrderId(orderId);
            logistics.setOrderNo(orderNo);
            logistics.setCompany(randomLogisticsCompany()); // 随机分配快递公司
            logistics.setTrackingNo(generateTrackingNo());  // 生成快递单号
            logistics.setStatus(0); // 初始状态：待揽收
            logistics.setReceiverAddress(address);
            logisticsMapper.insert(logistics);

            // 4. 创建初始物流轨迹（揽收节点）
            LogisticsTrack track = new LogisticsTrack();
            track.setLogisticsId(logistics.getId());
            track.setLocation("商家仓库");
            track.setRemark("商家已发货，快递员正在揽收");
            track.setTrackTime(LocalDateTime.now());
            track.setTrackType(1); // 揽收
            logisticsTrackMapper.insert(track);

            // 5. 发送物流操作日志到 Kafka（异步，不影响主流程）
            sendLogisticsLog(orderId, orderNo, logistics.getTrackingNo(), "物流单创建");

            log.info("物流单创建成功: orderId={}, trackingNo={}", orderId, logistics.getTrackingNo());

        } catch (Exception e) {
            log.error("处理订单支付消息失败: {}", message, e);
            // 抛出异常，RocketMQ 会重试消费（默认重试 16 次）
            throw new RuntimeException("处理订单支付消息失败", e);
        }
    }

    /**
     * 发送物流操作日志到 Kafka
     * 日志用于后续的数据分析和审计
     */
    private void sendLogisticsLog(Long orderId, String orderNo, String trackingNo, String action) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("orderId", orderId);
            logData.put("orderNo", orderNo);
            logData.put("trackingNo", trackingNo);
            logData.put("action", action);
            logData.put("timestamp", LocalDateTime.now().toString());

            String logJson = objectMapper.writeValueAsString(logData);
            // 使用 orderNo 作为 key，保证同一订单的日志有序
            kafkaTemplate.send(MqConstants.KAFKA_TOPIC_LOGISTICS_LOG, orderNo, logJson);
        } catch (Exception e) {
            // 日志发送失败不影响主业务
            log.error("物流日志发送失败: orderId={}", orderId, e);
        }
    }

    /**
     * 随机分配快递公司（模拟）
     */
    private String randomLogisticsCompany() {
        String[] companies = {"顺丰速运", "圆通速递", "中通快递", "韵达快递", "申通快递"};
        return companies[new Random().nextInt(companies.length)];
    }

    /**
     * 生成快递单号（模拟）
     * 格式：SF + 12位数字
     */
    private String generateTrackingNo() {
        return "SF" + System.currentTimeMillis() + (new Random().nextInt(900) + 100);
    }
}
