package com.storyforge.service.auth;

import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.domain.user.AuthDtos.UserResponse;
import com.storyforge.exception.ForbiddenException;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 계정 규칙: 관리자 판별, 이용약관 동의, 응답에 내려줄 내 정보.
 * 관리자는 DB 역할이 아니라 설정(ADMIN_EMAILS)으로 정합니다. 관리 화면에서 스스로 권한을 올릴 길을 두지 않기 위해서입니다.
 */
@Component
public class AccountPolicy {

    private final AppUserRepository users;
    private final Set<String> adminEmails;
    private final String termsVersion;
    private final boolean requireAdminVerification;

    public AccountPolicy(AppUserRepository users,
                         @Value("${storyforge.admin.emails}") String adminEmails,
                         @Value("${storyforge.terms.version}") String termsVersion,
                         @Value("${storyforge.mail.enabled:false}") boolean requireAdminVerification) {
        this.users = users;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.strip().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
        this.termsVersion = termsVersion;
        this.requireAdminVerification = requireAdminVerification;
    }

    public boolean isAdmin(AppUser user) {
        return adminEmails.contains(user.getEmail()) && (!requireAdminVerification || user.isEmailVerified());
    }

    public boolean isAdminId(Long userId) {
        return userId != null && users.findById(userId).map(this::isAdmin).orElse(false);
    }

    public void requireAdmin(Long userId) {
        if (!isAdminId(userId)) {
            throw new ForbiddenException("관리자만 할 수 있습니다.");
        }
    }

    public String termsVersion() {
        return termsVersion;
    }

    public boolean termsCurrent(AppUser user) {
        return termsVersion.equals(user.getTermsVersion());
    }

    /** 게시와 글쓰기 전에 현재 약관 동의를 확인합니다. */
    public void requireTerms(Long userId) {
        AppUser user = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (!termsCurrent(user)) {
            throw new InvalidStateException("이용약관에 동의해야 게시할 수 있습니다.");
        }
    }

    public UserResponse view(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getBio(), u.getWebsite(),
                u.getAvatarColor(), isAdmin(u), termsCurrent(u), u.isEmailVerified());
    }

    public List<String> adminEmails() {
        return List.copyOf(adminEmails);
    }
}
