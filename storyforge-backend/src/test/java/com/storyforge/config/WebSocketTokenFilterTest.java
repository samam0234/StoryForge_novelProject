package com.storyforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebSocketTokenFilterTest {

    @Test
    void copiesQueryTokenIntoBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ws");
        request.setServletPath("/api/v1/ws");
        request.setParameter("access_token", "token-1");
        AtomicReference<String> header = new AtomicReference<>();
        FilterChain chain = (req, res) -> header.set(((HttpServletRequest) req).getHeader("Authorization"));

        new WebSocketTokenFilter().doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals("Bearer token-1", header.get());
    }

    @Test
    void leavesOtherRequestsUntouched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/works");
        request.setServletPath("/api/v1/works");
        request.setParameter("access_token", "token-1");
        AtomicReference<String> header = new AtomicReference<>();
        FilterChain chain = (req, res) -> header.set(((HttpServletRequest) req).getHeader("Authorization"));

        new WebSocketTokenFilter().doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(header.get());
    }
}
