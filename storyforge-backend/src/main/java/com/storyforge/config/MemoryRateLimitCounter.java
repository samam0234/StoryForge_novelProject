package com.storyforge.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 단위 테스트용 메모리 창. 운영 필터는 PostgreSQL 창을 씁니다. */
public class MemoryRateLimitCounter implements RateLimitCounter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Window(long startMillis, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit, long nowMillis) {
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(window -> nowMillis - window.startMillis() >= WINDOW_MILLIS);
        }
        Window updated = windows.merge(key, new Window(nowMillis, 1), (old, first) ->
                nowMillis - old.startMillis() >= WINDOW_MILLIS ? first : new Window(old.startMillis(), old.count() + 1));
        return updated.count() <= limit;
    }
}
