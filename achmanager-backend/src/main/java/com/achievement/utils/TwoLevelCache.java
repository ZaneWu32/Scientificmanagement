package com.achievement.utils;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

//12.11 开发二级缓存工具类
@Component
@RequiredArgsConstructor
@Slf4j
public class TwoLevelCache {
    //依赖注入
    private final Cache<String, Object> caffeineCache;
    private final RedisTemplate<String, Object> redisTemplate;
    /**
            * 从二级缓存中获取数据：
            * 1. 先查 Caffeine
     * 2. 再查 Redis
     * 3. 最后查 DB（通过 dbLoader），并写入缓存
     * @param key        缓存 key
     * @param clazz      返回类型的 class（用于类型转换）
     * @param dbLoader   缓存未命中时，加载 DB 数据的逻辑
     * @param ttlSeconds Redis 中的过期时间（秒）
            */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz, Supplier<T> dbLoader, long ttlSeconds) {
        // 1. 查本地 Caffeine
        Object localVal = caffeineCache.getIfPresent(key);
        if (localVal != null) {
            return (T) localVal;
        }

        // 2. 查 Redis
        Object redisVal = redisTemplate.opsForValue().get(key);
        if (redisVal != null) {
            // 回填到本地缓存
            caffeineCache.put(key, redisVal);
            return (T) redisVal;
        }

        // 3. 查 DB
        T dbVal = dbLoader.get();
        if (dbVal != null) {
            // 回写缓存
            caffeineCache.put(key, dbVal);
            redisTemplate.opsForValue().set(key, dbVal, ttlSeconds, TimeUnit.SECONDS);
        }
        return dbVal;
    }

    /**
     * 删除某个 key 的缓存（本地 + Redis 都清）
     */
    public void evict(String key) {
        caffeineCache.invalidate(key);
        redisTemplate.delete(key);
    }
}