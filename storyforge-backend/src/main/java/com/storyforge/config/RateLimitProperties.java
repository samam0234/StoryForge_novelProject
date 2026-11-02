package com.storyforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param authPerMinute     IP당 로그인·가입 요청 수 (비밀번호 대입 방지)
 * @param generatePerMinute 사용자당 AI 생성 요청 수 (비용 초과 방지, 기획서 §15)
 */
@ConfigurationProperties(prefix = "storyforge.rate-limit")
public record RateLimitProperties(int authPerMinute, int generatePerMinute) {
}
