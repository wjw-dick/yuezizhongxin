package com.yzz.common.guard;

import java.time.Duration;

/**
 * 轻量缓存抽象，用于幂等键、限流计数与会话类数据。
 *
 * <p>生产环境走 Redis（key 统一带 tenant 前缀），单机调试可退化为内存实现。</p>
 */
public interface CacheStore {

    /** 键不存在时写入并返回 true；已存在返回 false。 */
    boolean setIfAbsent(String key, Duration ttl);

    /** 自增计数，首次写入时设置过期时间，返回自增后的值。 */
    long increment(String key, Duration ttl);

    void put(String key, String value, Duration ttl);

    String get(String key);

    void delete(String key);
}
