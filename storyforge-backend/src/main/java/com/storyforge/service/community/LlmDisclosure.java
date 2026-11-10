package com.storyforge.service.community;

import com.storyforge.domain.moderation.ModerationDtos.LlmNotice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 외부 언어 모델로 원고가 나가는지 알립니다.
 * 값은 AI 서비스와 같은 LLM_PROVIDER여야 합니다.
 */
@Component
public class LlmDisclosure {

    static final String EXTERNAL_NOTICE =
            "Story Bible, Outline, 이전 챕터 요약과 가이드 질문이 외부 언어 모델 제공자에게 전송됩니다.";

    private final String provider;

    public LlmDisclosure(@Value("${storyforge.llm.provider:mock}") String provider) {
        this.provider = provider == null || provider.isBlank() ? "mock" : provider.trim().toLowerCase(Locale.ROOT);
    }

    public LlmNotice notice() {
        boolean external = "anthropic".equals(provider);
        return new LlmNotice(provider, external, external ? EXTERNAL_NOTICE : null);
    }
}
