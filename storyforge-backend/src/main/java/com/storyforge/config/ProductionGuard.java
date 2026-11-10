package com.storyforge.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** prod에서 개발 자격 증명을 거절하고 웹·콘솔 각각의 정확한 출처를 검증합니다. */
@Component
@Profile("prod")
public class ProductionGuard {

    public ProductionGuard(Environment environment) {
        ProductionChecks.verify(
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"),
                environment.getProperty("storyforge.cors.frontend-origin"),
                environment.getProperty("storyforge.cors.console-origin"));
    }
}
