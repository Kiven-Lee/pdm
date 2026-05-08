package com.mall.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * <p>
 * 默认的 RedisTemplate 使用 JDK 序列化，存入 Redis 的数据不可读。
 * 此配置将序列化方式改为 JSON，使 Redis 中的数据可读可调试。
 * </p>
 *
 * 序列化策略：
 *   - key：StringRedisSerializer（字符串，可读）
 *   - value：Jackson2JsonRedisSerializer（JSON，可读）
 *   - hashKey：StringRedisSerializer
 *   - hashValue：Jackson2JsonRedisSerializer
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置 Jackson ObjectMapper，支持 Java8 时间类型（LocalDateTime 等）
        ObjectMapper objectMapper = new ObjectMapper();
        // 允许序列化所有字段（包括私有字段）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 序列化时写入类型信息，反序列化时可还原为原始类型
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        // 注册 Java8 时间模块，支持 LocalDateTime 序列化
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期序列化为时间戳（使用 ISO 格式字符串）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建 JSON 序列化器
        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        jsonSerializer.setObjectMapper(objectMapper);

        // key 和 hashKey 使用字符串序列化（保证 key 可读）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 和 hashValue 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 初始化 template
        template.afterPropertiesSet();
        return template;
    }
}
