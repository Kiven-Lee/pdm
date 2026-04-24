package com.mall.product.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品 Kafka 生产者
 * <p>
 * 负责发送商品相关的日志消息到 Kafka，用于：
 *   - 商品浏览量统计
 *   - 用户行为分析
 *   - 推荐系统数据采集
 * </p>
 *
 * 注意：Kafka 消息发送是异步的，不影响主业务流程。
 * 即使 Kafka 不可用，商品查询接口仍然正常工作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaProducer {

    /** Spring Kafka 模板，封装了 Kafka Producer API */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 发送商品浏览日志到 Kafka
     * <p>
     * 消息格式（JSON）：
     * {
     *   "productId": 1001,
     *   "userId": 2001,       // 未登录时为 null
     *   "viewTime": "2024-01-01T10:00:00",
     *   "source": "detail"    // 来源页面
     * }
     * </p>
     *
     * @param productId 被浏览的商品 ID
     * @param userId    浏览用户 ID（未登录时为 null）
     */
    public void sendProductViewLog(Long productId, Long userId) {
        try {
            // 构建日志消息体
            Map<String, Object> logData = new HashMap<>();
            logData.put("productId", productId);
            logData.put("userId", userId);
            logData.put("viewTime", LocalDateTime.now().toString());
            logData.put("source", "detail");

            // 将 Map 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(logData);

            // 异步发送到 Kafka，使用 productId 作为消息 key
            // 相同 key 的消息会路由到同一个 partition，保证同一商品的日志有序
            kafkaTemplate.send(MqConstants.KAFKA_TOPIC_PRODUCT_VIEW,
                    String.valueOf(productId), message)
                    .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                        @Override
                        public void onSuccess(SendResult<String, String> result) {
                            // 发送成功，记录 offset 信息（可选）
                            log.debug("商品浏览日志发送成功: productId={}, offset={}",
                                    productId, result.getRecordMetadata().offset());
                        }

                        @Override
                        public void onFailure(Throwable ex) {
                            // 发送失败，记录错误日志但不影响主业务
                            // 生产环境可以考虑写入本地文件或数据库作为补偿
                            log.error("商品浏览日志发送失败: productId={}, error={}",
                                    productId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            // JSON 序列化失败，记录日志但不抛出异常（不影响主业务）
            log.error("商品浏览日志序列化失败: productId={}", productId, e);
        }
    }
}
