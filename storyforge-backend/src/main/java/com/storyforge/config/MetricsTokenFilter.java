package com.storyforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Prometheus 스크랩은 METRICS_TOKEN 이 있을 때만 헤더로 허용합니다. */
public class MetricsTokenFilter extends OncePerRequestFilter {

    private final String expected;

    public MetricsTokenFilter(String expected) {
        this.expected = expected == null ? "" : expected;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"/actuator/prometheus".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String presented = request.getHeader("X-Metrics-Token");
        if (expected.isBlank() || presented == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }
}
