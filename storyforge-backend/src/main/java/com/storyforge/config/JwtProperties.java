package com.storyforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param secret     HS256 비밀 키. 비우면 시작할 때 무작위로 만듭니다 (재시작하면 토큰 무효).
 * @param ttl        액세스 토큰 유효 기간
 * @param refreshTtl 갱신 토큰 유효 기간. 비밀번호를 바꾸면 그 전에 발급한 토큰은 거부합니다.
 */
@ConfigurationProperties(prefix = "storyforge.jwt")
public record JwtProperties(String secret, Duration ttl, Duration refreshTtl) {
}
