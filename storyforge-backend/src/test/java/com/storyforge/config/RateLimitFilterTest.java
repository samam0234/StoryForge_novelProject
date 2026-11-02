package com.storyforge.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private Clock clock = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC);
    private final RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(2, 1), JsonMapper.builder().build(),
            new Clock() {
                @Override
                public java.time.ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return clock.instant();
                }
            });

    private int request(String method, String path, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setRequestURI(path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void limitsLoginByIpAndResetsAfterWindow() throws Exception {
        String login = "/api/v1/auth/login";
        assertThat(request("POST", login, "1.1.1.1")).isEqualTo(200);
        assertThat(request("POST", login, "1.1.1.1")).isEqualTo(200);
        assertThat(request("POST", login, "1.1.1.1")).isEqualTo(429);
        assertThat(request("POST", login, "2.2.2.2")).as("다른 IP는 별도").isEqualTo(200);

        clock = Clock.offset(clock, Duration.ofSeconds(61));
        assertThat(request("POST", login, "1.1.1.1")).as("창이 지나면 초기화").isEqualTo(200);
    }

    @Test
    void limitsGenerationButNotOtherRequests() throws Exception {
        assertThat(request("POST", "/api/v1/projects/1/bible/generate", "3.3.3.3")).isEqualTo(200);
        assertThat(request("POST", "/api/v1/projects/1/chapters/1/generate", "3.3.3.3")).isEqualTo(429);
        for (int i = 0; i < 5; i++) {
            assertThat(request("GET", "/api/v1/projects", "3.3.3.3")).isEqualTo(200);
            assertThat(request("PUT", "/api/v1/projects/1/bible", "3.3.3.3")).isEqualTo(200);
        }
    }
}
