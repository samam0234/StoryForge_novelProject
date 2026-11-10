package com.storyforge.service.auth;

import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/** 메일 토큰은 원문을 저장하지 않으며 사용자 행 잠금으로 재발급·소비를 직렬화합니다. */
@Service
public class AccountMailService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entities;
    private final JdbcTemplate jdbc;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private final boolean enabled;
    private final String from;
    private final String origin;
    private final SecureRandom random = new SecureRandom();

    public AccountMailService(JdbcTemplate jdbc, AppUserRepository users, PasswordEncoder encoder, JavaMailSender mail,
            @Value("${storyforge.mail.enabled:false}") boolean enabled,
            @Value("${storyforge.mail.from:}") String from,
            @Value("${storyforge.mail.frontend-origin:http://localhost:4200}") String origin) {
        this.jdbc = jdbc; this.users = users; this.encoder = encoder; this.mail = mail;
        this.enabled = enabled; this.from = from; this.origin = origin;
        if (enabled) {
            java.net.URI uri = java.net.URI.create(origin);
            boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
            if (!("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme())))
                    || uri.getHost() == null || uri.getRawQuery() != null || uri.getFragment() != null
                    || uri.getUserInfo() != null || !uri.getPath().isEmpty() || from.isBlank()
                    || from.contains("\r") || from.contains("\n")) {
                throw new IllegalStateException("메일 발신 주소와 경로 없는 HTTPS 출처가 필요합니다. 로컬 HTTP는 허용합니다.");
            }
        }
    }

    public boolean enabled() { return enabled; }

    @Transactional
    public void requestReset(String email) {
        requireEnabled();
        users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(u -> issue(u.getId(), "reset"));
    }

    @Transactional
    public void requestVerification(Long id) {
        requireEnabled();
        issue(id, "verify");
    }

    private AppUser lock(Long id) {
        jdbc.queryForList("select id from app_user where id = ? for update", id);
        AppUser user = users.findById(id).orElseThrow(() -> new InvalidStateException("유효하지 않은 요청입니다."));
        entities.refresh(user);
        return user;
    }

    private void issue(Long id, String purpose) {
        AppUser user = lock(id);
        if (user.isSuspended() || (purpose.equals("verify") && user.isEmailVerified())) return;
        Instant now = Instant.now();
        Integer recent = jdbc.queryForObject("select count(*) from account_mail_token where user_id = ? and purpose = ? and created_at > ?",
                Integer.class, id, purpose, Timestamp.from(now.minusSeconds(60)));
        if (recent != null && recent > 0) return;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("delete from account_mail_token where user_id = ? and purpose = ?", id, purpose);
        jdbc.update("insert into account_mail_token (user_id, purpose, token_hash, credential_version, created_at, expires_at) values (?, ?, ?, ?, ?, ?)",
                id, purpose, hash(token), user.getCredentialVersion(), Timestamp.from(now),
                Timestamp.from(now.plusSeconds(purpose.equals("reset") ? 1800 : 86400)));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(user.getEmail());
        message.setSubject(purpose.equals("reset") ? "StoryForge 비밀번호 재설정" : "StoryForge 이메일 인증");
        // fragment는 서버 접근 로그와 Referer로 토큰을 전달하지 않습니다.
        message.setText("아래 링크에서 요청을 완료하세요. 직접 요청하지 않았다면 무시하세요.\n"
                + origin + "/account-mail#" + purpose + ":" + token
                + "\n유효 시간: " + (purpose.equals("reset") ? "30분" : "24시간"));
        try {
            mail.send(message);
        } catch (MailException ex) {
            jdbc.update("delete from account_mail_token where user_id = ? and purpose = ?", id, purpose);
            // 공개 재설정 API는 메일 장애로 계정 존재 여부를 노출하지 않습니다.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("계정 메일 발송 실패: {}", ex.getClass().getSimpleName());
        }
    }

    @Transactional
    public void consume(String token, String purpose, String password) {
        requireEnabled();
        String digest = hash(token);
        var ids = jdbc.queryForList("select user_id from account_mail_token where token_hash = ? and purpose = ?", Long.class, digest, purpose);
        if (ids.isEmpty()) throw invalid();
        AppUser user = lock(ids.getFirst());
        Integer valid = jdbc.queryForObject("select count(*) from account_mail_token where token_hash = ? and purpose = ? and expires_at > ? and credential_version = ?",
                Integer.class, digest, purpose, Timestamp.from(Instant.now()), user.getCredentialVersion());
        if (valid == null || valid != 1 || user.isSuspended()) throw invalid();
        if (purpose.equals("reset")) {
            if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new InvalidStateException("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
            }
            user.changePassword(encoder.encode(password));
            jdbc.update("delete from account_mail_token where user_id = ?", user.getId());
        } else {
            user.verifyEmail();
            jdbc.update("delete from account_mail_token where token_hash = ?", digest);
        }
    }

    private void requireEnabled() {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "현재 메일 기능을 사용할 수 없습니다.");
    }

    private static InvalidStateException invalid() { return new InvalidStateException("만료되었거나 이미 사용한 링크입니다. 다시 요청하세요."); }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
