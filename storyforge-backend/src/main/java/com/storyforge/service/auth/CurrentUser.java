package com.storyforge.service.auth;

import com.storyforge.exception.AuthenticationFailedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 요청 스레드의 인증 사용자. 비동기 스트림 안(다른 스레드)에서는 쓰지 않습니다.
 */
@Component
public class CurrentUser {

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        throw new AuthenticationFailedException("로그인이 필요합니다.");
    }
}
