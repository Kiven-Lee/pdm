package com.mall.logistics.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 物流日志 Kafka 消费者
 * <p>
 * 消费 Kafka 中的物流操作日志，用于：
 *   - 日志持久化（写入 ES 或数据库）
 *   - 实时监控（异常物流告警）
 *   - 数据分析（物流时效统计）
 *
 * 此处简化实现，仅打印日志。
 * 生产环境可以接入 ELK（Elasticsearch + Logstash + Kibana）。
 * </p>
 */
@Slf4j
@Component
public class LogisticsLogConsumer {

    /**
     * 消费物流操作日志
     * <p>
     * @KafkaListener 参数说明：
     *   - topics：订阅的 Topic 列表
     *   - groupId：消费者组 ID
     *   - containerFactory：消费者工厂（使用默认配置）
     * </p>
     *
     * @param record Kafka 消息记录，包含 key、value、partition、offset 等信息
     */
    @KafkaListener(
            topics = "logistics-operation-log",
            groupId = "logistics-log-consumer-group"
    )
    public void consumeLogisticsLog(ConsumerRecord<String, String> record) {
        // 记录消息的元数据信息（partition 和 offset 用于消息追踪）
        log.info("收到物流日志消息: partition={}, offset={}, key={}, value={}",
                record.partition(),
                record.offset(),
                record.key(),
                record.value());

        // TODO: 生产环境在此处将日志写入 Elasticsearch 或数据库
        // elasticsearchService.saveLog(record.value());
    }

    /**
     * 消费商品浏览日志（来自商品服务）
     * 物流服务也可以消费此日志用于分析热门商品的物流需求
     */
    @KafkaListener(
            topics = "product-view-log",
            groupId = "logistics-product-view-group"
    )
    public void consumeProductViewLog(ConsumerRecord<String, String> record) {
        log.debug("收到商品浏览日志: key={}, value={}", record.key(), record.value());
        // 此处可以统计热门商品，提前备货到就近仓库
    }
}
