package com.storyforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 브라우저는 웹소켓 업그레이드에 Authorization 헤더를 넣지 못합니다.
 * 쿼리의 access_token을 Bearer 헤더로 옮겨 기존 JWT 필터가 검증하게 합니다.
 */
public class WebSocketTokenFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return !"/api/v1/ws".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = request.getParameter("access_token");
        if ((header == null || header.isBlank()) && token != null && !token.isBlank()) {
            filterChain.doFilter(new BearerRequest(request, token), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class BearerRequest extends HttpServletRequestWrapper {
        private final String token;

        private BearerRequest(HttpServletRequest request, String token) {
            super(request);
            this.token = token;
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return "Bearer " + token;
            }
            return super.getHeader(name);
        }
    }
}
