package com.storyforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param authPerMinute     IP당 로그인·가입 요청 수 (비밀번호 대입 방지)
 * @param generatePerMinute 사용자당 AI 생성·가이드챗 요청 수 (비용 초과 방지, 기획서 §15)
 * @param communityPerMinute 사용자당 글·댓글·공유·Clone 요청 수, IP당 공유 방문 수 (도배 방지)
 */
@ConfigurationProperties(prefix = "storyforge.rate-limit")
public record RateLimitProperties(int authPerMinute, int generatePerMinute, int communityPerMinute) {
}
