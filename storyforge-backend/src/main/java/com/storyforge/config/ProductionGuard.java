package com.storyforge.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** prod 프로필에서만 개발 기본 자격 증명과 여러 CORS 출처를 거절합니다. */
@Component
@Profile("prod")
public class ProductionGuard {

    public ProductionGuard(Environment environment) {
        ProductionChecks.verify(
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"),
                environment.getProperty("storyforge.cors.allowed-origins"));
    }
}
