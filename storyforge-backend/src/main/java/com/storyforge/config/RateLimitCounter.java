package com.storyforge.config;

/** 1분 고정 창에서 한 번 통과시킬지 판단합니다. */
public interface RateLimitCounter {

    boolean tryAcquire(String key, int limit, long nowMillis);
}
