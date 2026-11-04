package com.storyforge.service.auth;

import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.domain.user.AuthDtos.AuthResponse;
import com.storyforge.domain.user.AuthDtos.LoginRequest;
import com.storyforge.domain.user.AuthDtos.RegisterRequest;
import com.storyforge.domain.user.AuthDtos.UserResponse;
import com.storyforge.config.AdminDirectory;
import com.storyforge.domain.user.Role;
import com.storyforge.exception.AuthenticationFailedException;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final AdminDirectory admins;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokens, AdminDirectory admins) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.admins = admins;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new InvalidStateException("비밀번호가 너무 깁니다.");
        }
        if (users.existsByEmail(email)) {
            throw new InvalidStateException("이미 가입된 이메일입니다.");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), request.displayName().trim());
        user.acceptTerms();
        if (admins.contains(email)) {
            user.grantAdmin();
        }
        return tokens.issue(Objects.requireNonNull(users.save(user)));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = users.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (admins.contains(email) && user.getRole() != Role.ADMIN) {
            user.grantAdmin();
        }
        return tokens.issue(user);
    }

    public UserResponse me(Long userId) {
        return users.findById(Objects.requireNonNull(userId))
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
