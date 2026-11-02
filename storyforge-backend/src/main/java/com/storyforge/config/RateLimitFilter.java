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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1분 고정 창 Rate Limiting (기획서 §13). 메모리 기반이라 서버 한 대 기준입니다.
 * 로그인·가입은 IP별, AI 생성은 사용자별로 셉니다. 다른 요청은 제한하지 않습니다.
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

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = null;
        int limit = 0;
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod())) {
            if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")) {
                key = "auth:" + request.getRemoteAddr();
                limit = properties.authPerMinute();
            } else if (path.startsWith("/api/v1/") && path.endsWith("/generate")) {
                key = "generate:" + principal(request);
                limit = properties.generatePerMinute();
            }
        }
        if (key != null && !tryAcquire(key, limit)) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static String principal(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : request.getRemoteAddr();
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
