package com.yzz.common.guard;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存缓存实现，默认用于本地开发与单机部署。
 *
 * <p>多实例部署时必须切换为 Redis，否则限流与幂等只在单机生效。</p>
 */
public class MemoryCacheStore implements CacheStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public boolean setIfAbsent(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        long expireAt = now + ttl.toMillis();
        AtomicBoolean created = new AtomicBoolean(false);
        store.compute(key, (k, old) -> {
            if (old == null || old.expireAt <= now) {
                created.set(true);
                return new Entry("1", expireAt);
            }
            return old;
        });
        return created.get();
    }

    @Override
    public long increment(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        long expireAt = now + ttl.toMillis();
        Entry entry = store.compute(key, (k, old) -> {
            if (old == null || old.expireAt <= now) {
                Entry fresh = new Entry("0", expireAt);
                fresh.counter.incrementAndGet();
                return fresh;
            }
            old.counter.incrementAndGet();
            return old;
        });
        return entry.counter.get();
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt <= System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    private static final class Entry {
        private final String value;
        private final long expireAt;
        private final AtomicLong counter = new AtomicLong(0);

        private Entry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
