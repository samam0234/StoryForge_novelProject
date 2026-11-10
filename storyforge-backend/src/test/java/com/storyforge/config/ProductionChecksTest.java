package com.storyforge.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionChecksTest {

    @Test
    void acceptsExplicitWebAndConsoleHttpsOrigins() {
        ProductionChecks.verify("app", "secret-value", "https://storyforge.example", "https://console.storyforge.example");
    }

    @Test
    void rejectsMissingWildcardAndNonOriginConsoleValues() {
        for (String value : new String[]{"", "http://console.example", "https://*.example", "https://console.example/path",
                "https://console.example/", "https://console.example?q=x", "https://console.example#x",
                "https://admin:password@console.example", "https://a.example,https://b.example"}) {
            assertThatThrownBy(() -> ProductionChecks.verify("app", "secret-value", "https://web.example", value))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void rejectsDevelopmentDefaultsAndMultipleOrigins() {
        assertThatThrownBy(() -> ProductionChecks.verify("storyforge", "secret-value", "https://storyforge.example"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionChecks.verify("app", "storyforge", "https://storyforge.example"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionChecks.verify("app", "secret-value", "https://a.example,https://b.example"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsOneOriginAndReplacedCredentials() {
        ProductionChecks.verify("app", "secret-value", "https://storyforge.example");
    }

    @Test
    void productionRequiresAFixedJwtSecret() {
        assertThatThrownBy(() -> JwtConfig.keyMaterial("", true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> JwtConfig.keyMaterial("short", false)).isInstanceOf(IllegalStateException.class);
        assertThat(JwtConfig.keyMaterial("x".repeat(32), true)).hasSize(32);
        assertThat(JwtConfig.keyMaterial("", false)).hasSize(32);
    }
}
