package com.storyforge.controller;

import com.storyforge.support.IntegrationTestBase;
import com.storyforge.support.LocalSmtpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

class AccountMailIntegrationTest extends IntegrationTestBase {
    static final LocalSmtpServer SMTP = new LocalSmtpServer();
    @Autowired JdbcTemplate jdbc;
    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry r) {
        r.add("storyforge.mail.enabled", () -> true);
        r.add("storyforge.mail.from", () -> "noreply@example.test");
        r.add("spring.mail.host", () -> "localhost");
        r.add("spring.mail.port", SMTP::port);
    }
    @BeforeEach void clearMail() { SMTP.clear(); }
    private String[] account() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String jwt = call("POST", "/auth/register", Map.of("email", email, "password", "password123",
                "displayName", "메일 검증", "acceptTerms", true), null).json().get("accessToken").asString();
        return new String[]{email, jwt};
    }
    private String request(String email) throws Exception {
        assertThat(call("POST", "/auth/mail/reset-request", Map.of("email", email), null).status()).isEqualTo(200);
        return token(SMTP.body(), "reset");
    }
    private String token(String body, String purpose) {
        var match = java.util.regex.Pattern.compile("#" + purpose + ":([A-Za-z0-9_-]{43})").matcher(body);
        assertThat(match.find()).as(body).isTrue();
        return match.group(1);
    }
    @Test void smtpResetIsOneTimeAndRevokesOldSessions() throws Exception {
        var user = account();
        String token = request(user[0]);
        assertThat(jdbc.queryForObject("select count(*) from account_mail_token where token_hash = ?", Integer.class, token)).isZero();
        assertThat(call("POST", "/auth/mail/reset", Map.of("token", token, "password", "replacement123"), null).status()).isEqualTo(200);
        assertThat(call("POST", "/auth/mail/reset", Map.of("token", token, "password", "replacement456"), null).status()).isEqualTo(409);
        assertThat(call("GET", "/auth/me", null, user[1]).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/login", Map.of("email", user[0], "password", "password123"), null).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/login", Map.of("email", user[0], "password", "replacement123"), null).status()).isEqualTo(200);
    }
    @Test void verifyMailDoesNotResetPasswordAndIsSingleUse() throws Exception {
        var user = account();
        assertThat(call("POST", "/auth/mail/verify-request", null, null).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/mail/verify-request", null, user[1]).status()).isEqualTo(200);
        String token = token(SMTP.body(), "verify");
        assertThat(call("POST", "/auth/mail/reset", Map.of("token", token, "password", "replacement123"), null).status()).isEqualTo(409);
        assertThat(call("POST", "/auth/mail/verify", Map.of("token", token), null).status()).isEqualTo(200);
        assertThat(call("GET", "/auth/me", null, user[1]).json().get("emailVerified").asBoolean()).isTrue();
        assertThat(call("POST", "/auth/mail/verify", Map.of("token", token), null).status()).isEqualTo(409);
    }
    @Test void expiredTokensFailAndRequestsAreThrottledWithoutDisclosingAccounts() throws Exception {
        var user = account();
        String token = request(user[0]);
        Response known = call("POST", "/auth/mail/reset-request", Map.of("email", user[0]), null);
        Response unknown = call("POST", "/auth/mail/reset-request", Map.of("email", "missing@example.test"), null);
        assertThat(known.body()).isEqualTo(unknown.body());
        assertThat(SMTP.size()).isZero();
        jdbc.update("update account_mail_token set expires_at = now() - interval '1 second' where user_id = (select id from app_user where email = ?)", user[0]);
        assertThat(call("POST", "/auth/mail/reset", Map.of("token", token, "password", "replacement123"), null).status()).isEqualTo(409);
    }
    @Test void concurrentConsumptionHasOnlyOneWinner() throws Exception {
        String token = request(account()[0]);
        var a = CompletableFuture.supplyAsync(() -> reset(token));
        var b = CompletableFuture.supplyAsync(() -> reset(token));
        assertThat(java.util.List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
    }
    @Test void reissueInvalidatesPreviousLinkAndLongPasswordDoesNotConsumeNewLink() throws Exception {
        var user = account();
        String old = request(user[0]);
        jdbc.update("update account_mail_token set created_at = now() - interval '2 minutes' where user_id = (select id from app_user where email = ?)", user[0]);
        String fresh = request(user[0]);
        assertThat(reset(old)).isEqualTo(409);
        assertThat(call("POST", "/auth/mail/reset", Map.of("token", fresh, "password", "가".repeat(25)), null).status()).isEqualTo(409);
        assertThat(reset(fresh)).isEqualTo(200);
    }
    @Test void smtpFailureDoesNotLeaveAUsableTokenOrDiscloseAccountInResponse() throws Exception {
        var user = account();
        SMTP.rejectNext = true;
        Response failed = call("POST", "/auth/mail/reset-request", Map.of("email", user[0]), null);
        Response unknown = call("POST", "/auth/mail/reset-request", Map.of("email", "missing@example.test"), null);
        assertThat(failed.status()).isEqualTo(200);
        assertThat(failed.body()).isEqualTo(unknown.body());
        assertThat(jdbc.queryForObject("select count(*) from account_mail_token where user_id = (select id from app_user where email = ?)", Integer.class, user[0])).isZero();
        assertThat(SMTP.size()).isZero();
        assertThat(reset(request(user[0]))).isEqualTo(200);
    }
    private int reset(String token) {
        try { return call("POST", "/auth/mail/reset", Map.of("token", token, "password", "replacement123"), null).status(); }
        catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
