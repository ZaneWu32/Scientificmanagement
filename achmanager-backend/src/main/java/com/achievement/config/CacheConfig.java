package com.achievement.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine + Redis 基础配置
 */
@Configuration
@EnableCaching   // 先打开缓存能力，后续如果用 @Cacheable 也方便
public class CacheConfig {

    /**
     * 本地 Caffeine 缓存
     */
    @Bean
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)              // 本地最多缓存多少个 key
                .expireAfterWrite(10, TimeUnit.MINUTES) // 默认写入 10 分钟过期（可以再在 TwoLevelCache 控制 TTL）
                .build();
    }

    /**
     * RedisTemplate：使用 JSON 序列化存储对象
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 用字符串
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // value 用 JSON，带类型信息
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
