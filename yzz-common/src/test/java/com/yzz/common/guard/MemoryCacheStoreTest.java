package com.yzz.common.guard;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryCacheStoreTest {

    private final MemoryCacheStore store = new MemoryCacheStore();

    @Test
    void 同一幂等键只放行一次() {
        assertTrue(store.setIfAbsent("idem:1:key", Duration.ofSeconds(10)));
        assertFalse(store.setIfAbsent("idem:1:key", Duration.ofSeconds(10)));
    }

    @Test
    void 计数在窗口内累加() {
        assertEquals(1, store.increment("rate:1:/api/order:100", Duration.ofSeconds(60)));
        assertEquals(2, store.increment("rate:1:/api/order:100", Duration.ofSeconds(60)));
    }

    @Test
    void 过期后键失效() throws Exception {
        store.put("tenant:state:demo", "NORMAL", Duration.ofMillis(80));
        assertEquals("NORMAL", store.get("tenant:state:demo"));
        Thread.sleep(150);
        assertEquals(null, store.get("tenant:state:demo"));
    }
}
