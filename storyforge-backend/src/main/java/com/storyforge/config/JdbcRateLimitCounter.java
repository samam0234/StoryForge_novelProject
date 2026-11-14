package com.storyforge.config;

import org.springframework.jdbc.core.JdbcTemplate;

/** 서버 여러 대가 PostgreSQL의 같은 1분 창을 공유합니다. */
public class JdbcRateLimitCounter implements RateLimitCounter {

    private static final long WINDOW_MILLIS = 60_000;

    private final JdbcTemplate jdbc;

    public JdbcRateLimitCounter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryAcquire(String key, int limit, long nowMillis) {
        jdbc.update("delete from rate_limit_window where window_start + ? < ?", WINDOW_MILLIS * 2, nowMillis);
        Long hits = jdbc.queryForObject("""
                insert into rate_limit_window (bucket, window_start, hits)
                values (?, ?, 1)
                on conflict (bucket) do update set
                    hits = case when rate_limit_window.window_start + ? <= ? then 1 else rate_limit_window.hits + 1 end,
                    window_start = case when rate_limit_window.window_start + ? <= ? then ? else rate_limit_window.window_start end
                returning hits
                """, Long.class, key, nowMillis, WINDOW_MILLIS, nowMillis, WINDOW_MILLIS, nowMillis, nowMillis);
        return hits != null && hits <= limit;
    }
}
