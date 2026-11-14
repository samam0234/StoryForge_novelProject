package com.storyforge.service.auth;

import com.storyforge.config.JwtProperties;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.AuthenticationFailedException;
import com.storyforge.exception.ForbiddenException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/** 갱신 토큰은 SHA-256만 저장하고, 한 번 쓰면 폐기한 뒤 새 토큰을 발급합니다. */
@Service
public class RefreshTokenService {

    public record Issued(String token, Instant expiresAt) {
    }

    private final JdbcTemplate jdbc;
    private final AppUserRepository users;
    private final JwtProperties properties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(JdbcTemplate jdbc, AppUserRepository users, JwtProperties properties) {
        this.jdbc = jdbc;
        this.users = users;
        this.properties = properties;
    }

    @Transactional
    public Issued create(AppUser user) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.refreshTtl());
        jdbc.update("""
                insert into refresh_token (user_id, token_hash, credential_version, expires_at, created_at)
                values (?, ?, ?, ?, ?)
                """, user.getId(), hash(token), user.getCredentialVersion(), Timestamp.from(expiresAt), Timestamp.from(now));
        return new Issued(token, expiresAt);
    }

    /** 유효한 토큰을 폐기하고 사용자를 돌려줍니다. 호출한 쪽에서 새 쌍을 발급합니다. */
    @Transactional
    public AppUser consume(String raw) {
        String digest = hash(raw);
        var rows = jdbc.queryForList("""
                select user_id, credential_version, expires_at, revoked_at
                from refresh_token where token_hash = ? for update
                """, digest);
        if (rows.isEmpty()) {
            throw new AuthenticationFailedException("다시 로그인해 주세요.");
        }
        Map<String, Object> row = rows.getFirst();
        if (row.get("revoked_at") != null || !instant(row.get("expires_at")).isAfter(Instant.now())) {
            throw new AuthenticationFailedException("다시 로그인해 주세요.");
        }
        long userId = ((Number) row.get("user_id")).longValue();
        long version = ((Number) row.get("credential_version")).longValue();
        AppUser user = users.findById(userId).orElseThrow(() -> new AuthenticationFailedException("다시 로그인해 주세요."));
        if (user.getCredentialVersion() != version) {
            throw new AuthenticationFailedException("다시 로그인해 주세요.");
        }
        if (user.isSuspended()) {
            throw new ForbiddenException("이용이 정지된 계정입니다.");
        }
        jdbc.update("update refresh_token set revoked_at = ? where token_hash = ?", Timestamp.from(Instant.now()), digest);
        return user;
    }

    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        jdbc.update("update refresh_token set revoked_at = ? where token_hash = ? and revoked_at is null",
                Timestamp.from(Instant.now()), hash(raw));
    }

    @Transactional
    public void revokeAll(Long userId) {
        jdbc.update("update refresh_token set revoked_at = ? where user_id = ? and revoked_at is null",
                Timestamp.from(Instant.now()), userId);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Instant instant(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("갱신 토큰의 시각을 읽지 못했습니다.");
    }
}
