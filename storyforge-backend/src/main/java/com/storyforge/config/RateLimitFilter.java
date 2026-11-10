package com.storyforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1분 고정 창 Rate Limiting (기획서 §13). 메모리 기반이라 서버 한 대 기준입니다.
 * 로그인·가입은 IP별, AI 생성·가이드챗과 커뮤니티 작성은 사용자별로 셉니다. 공유 방문은 IP별로 셉니다.
 * 신뢰하는 프록시 뒤에서만 X-Forwarded-For의 실제 클라이언트를 씁니다.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Window(long startMillis, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Set<String> trustedProxies;

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, Clock clock, Set<String> trustedProxies) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.trustedProxies = trustedProxies == null ? Set.of() : Set.copyOf(trustedProxies);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = null;
        int limit = 0;
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod())) {
            if (path.startsWith("/api/v1/auth/")) {
                key = "auth:" + ClientAddresses.resolve(request, trustedProxies);
                limit = properties.authPerMinute();
            } else if (path.startsWith("/api/v1/") && (path.endsWith("/generate") || path.endsWith("/continuity") || path.equals("/api/v1/guide/chat"))) {
                key = "generate:" + principal(request);
                limit = properties.generatePerMinute();
            } else if (path.matches("/api/v1/shares/[^/]+/visit")) {
                key = "visit:" + ClientAddresses.resolve(request, trustedProxies);
                limit = properties.communityPerMinute();
            } else if (path.equals("/api/v1/posts") || path.equals("/api/v1/reports")
                    || path.matches("/api/v1/(works|posts)/[^/]+/comments")
                    || path.matches("/api/v1/works/[^/]+/(shares|clone)")) {
                key = "community:" + principal(request);
                limit = properties.communityPerMinute();
            }
        } else if ("PUT".equals(request.getMethod()) && path.matches("/api/v1/users/[^/]+/block")) {
            key = "community:" + principal(request);
            limit = properties.communityPerMinute();
        }
        if (key != null && !tryAcquire(key, limit)) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private String principal(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : ClientAddresses.resolve(request, trustedProxies);
    }

    private boolean tryAcquire(String key, int limit) {
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(w -> now - w.startMillis() >= WINDOW_MILLIS);
        }
        Window updated = windows.merge(key, new Window(now, 1), (old, first) ->
                now - old.startMillis() >= WINDOW_MILLIS ? first : new Window(old.startMillis(), old.count() + 1));
        return updated.count() <= limit;
    }

    private void reject(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
