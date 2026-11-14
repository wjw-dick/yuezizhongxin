package com.yzz.common.guard;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存实现装配。通过 yzz.cache.provider 切换：memory（默认）/ redis。
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "yzz.cache.provider", havingValue = "redis")
    public CacheStore redisCacheStore(StringRedisTemplate redisTemplate) {
        return new RedisCacheStore(redisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "yzz.cache.provider", havingValue = "memory", matchIfMissing = true)
    public CacheStore memoryCacheStore() {
        return new MemoryCacheStore();
    }
}
