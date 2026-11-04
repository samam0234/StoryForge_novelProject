package com.storyforge.config;

import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.AuthenticationFailedException;
import com.storyforge.exception.ForbiddenException;
import com.storyforge.service.auth.CurrentUser;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 쓰기 요청 전에 계정 상태를 확인합니다. JWT는 만료 전까지 유효하므로,
 * 정지되었거나 삭제된 계정의 토큰은 여기서 막습니다. 읽기는 막지 않습니다.
 */
@Component
public class AccountGuardInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final AppUserRepository users;
    private final CurrentUser currentUser;

    public AccountGuardInterceptor(AppUserRepository users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() != DispatcherType.REQUEST || READ_METHODS.contains(request.getMethod())) {
            return true;
        }
        Long userId = currentUser.idOrNull();
        if (userId == null) {
            return true;
        }
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("로그인이 필요합니다."));
        if (user.isSuspended()) {
            throw new ForbiddenException("이용이 정지된 계정입니다." + (user.getSuspendedReason() == null ? "" : " 사유: " + user.getSuspendedReason()));
        }
        return true;
    }
}
