package com.storyforge.service.moderation;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;

    private long userId(String token) throws Exception {
        return call("GET", "/auth/me", null, token).json().get("id").asLong();
    }

    private long report(String type, long id, String token) throws Exception {
        var response = call("POST", "/reports", Map.of("targetType", type, "targetId", id, "reason", "SPAM"), token);
        assertThat(response.status()).isEqualTo(201);
        return response.json().get("id").asLong();
    }

    private long post(String token) throws Exception {
        return call("POST", "/posts", Map.of("category", "FREE", "title", "검토 대상", "content", "내용"), token).json().get("id").asLong();
    }

    @Test
    void consoleRequiresAdminOnEveryReadAndWrite() throws Exception {
        String user = newUser();
        for (String path : List.of("/admin/overview", "/admin/users", "/admin/audit")) {
            assertThat(call("GET", path, null, null).status()).isEqualTo(401);
            assertThat(call("GET", path, null, user).status()).isEqualTo(403);
            assertThat(call("GET", path, null, adminToken()).status()).isEqualTo(200);
        }
        assertThat(call("PUT", "/admin/users/1/suspension", Map.of("suspended", true, "note", "사유"), user).status()).isEqualTo(403);
        assertThat(call("POST", "/admin/reports/batch", Map.of("reportIds", List.of(1), "action", "DISMISS", "note", "사유"), user).status()).isEqualTo(403);
    }

    @Test
    void memberSearchPagingSuspensionAndAuditWorkTogether() throws Exception {
        String member = newUser();
        long id = userId(member);
        String admin = adminToken();
        String marker = "검색%_" + UUID.randomUUID();
        jdbc.update("UPDATE app_user SET display_name = ? WHERE id = ?", marker, id);
        var found = call("GET", "/admin/users?q=" + URLEncoder.encode(marker, StandardCharsets.UTF_8) + "&size=200", null, admin).json();
        assertThat(found.get("total").asLong()).isEqualTo(1);
        assertThat(found.get("size").asInt()).isEqualTo(50);
        assertThat(found.get("items").get(0).has("passwordHash")).isFalse();
        assertThat(call("PUT", "/admin/users/" + id + "/suspension", Map.of("suspended", true, "note", " 반복 도배 "), admin).status()).isEqualTo(200);
        assertThat(call("POST", "/projects", Map.of("title", "차단 확인"), member).status()).isEqualTo(403);
        assertThat(call("GET", "/admin/users?q=" + URLEncoder.encode(marker, StandardCharsets.UTF_8) + "&suspended=true", null, admin).json().get("total").asLong()).isEqualTo(1);
        assertThat(call("PUT", "/admin/users/" + id + "/suspension", Map.of("suspended", false, "note", "재검토"), admin).status()).isEqualTo(200);
        assertThat(call("POST", "/projects", Map.of("title", "정상"), member).status()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit WHERE target_type = 'USER' AND target_id = ?", Long.class, id)).isEqualTo(2L);
        var audit = call("GET", "/admin/audit?actorId=" + userId(admin), null, admin).json();
        assertThat(audit.get("items").get(0).get("note").asString()).isEqualTo("재검토");
    }

    @Test
    void administratorAndInvalidRequestsAreProtected() throws Exception {
        String admin = adminToken();
        long id = userId(admin);
        assertThat(call("PUT", "/admin/users/" + id + "/suspension", Map.of("suspended", true, "note", "검토"), admin).status()).isEqualTo(409);
        assertThat(call("PUT", "/admin/users/" + id + "/suspension", Map.of("suspended", true, "note", " "), admin).status()).isEqualTo(400);
        assertThat(call("POST", "/admin/reports/batch", Map.of("reportIds", List.of(), "action", "DISMISS", "note", "검토"), admin).status()).isEqualTo(400);
        assertThat(call("POST", "/admin/reports/batch", Map.of("reportIds", java.util.Collections.nCopies(51, 1), "action", "DISMISS", "note", "검토"), admin).status()).isEqualTo(400);
        assertThat(call("GET", "/admin/users?q=" + "x".repeat(201), null, admin).status()).isEqualTo(400);
    }

    @Test
    void duplicateTargetBatchProducesOneActionAndOneAudit() throws Exception {
        String author = newUser();
        long post = post(author);
        long first = report("POST", post, newUser());
        long second = report("POST", post, newUser());
        var result = call("POST", "/admin/reports/batch", Map.of("reportIds", List.of(first, first, second), "action", "HIDE", "note", "정책 위반"), adminToken());
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.json().get("requested").asInt()).isEqualTo(2);
        assertThat(result.json().get("processed").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit WHERE target_type = 'POST' AND target_id = ?", Long.class, post)).isEqualTo(1L);
        assertThat(call("PUT", "/admin/reports/" + second, Map.of("action", "HIDE"), adminToken()).status()).isEqualTo(409);
        assertThat(call("PUT", "/admin/reports/" + first, Map.of("action", "RESTORE", "note", "복원"), adminToken()).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit WHERE target_type = 'POST' AND target_id = ?", Long.class, post)).isEqualTo(2L);
    }

    @Test
    void failedBatchRollsBackChangesNotificationsAndAudit() throws Exception {
        String author = newUser();
        String reader = newUser();
        long authorId = userId(author);
        long post = post(author);
        long first = report("POST", post, reader);
        long invalid = report("USER", authorId, reader);
        long notifications = jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ?", Long.class, authorId);
        assertThat(call("POST", "/admin/reports/batch", Map.of("reportIds", List.of(first, invalid), "action", "HIDE", "note", "검토"), adminToken()).status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT hidden_at IS NULL FROM post WHERE id = ?", Boolean.class, post)).isTrue();
        assertThat(jdbc.queryForObject("SELECT status FROM content_report WHERE id = ?", String.class, first)).isEqualTo("OPEN");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit WHERE target_type = 'POST' AND target_id = ?", Long.class, post)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ?", Long.class, authorId)).isEqualTo(notifications);
    }

    @Test
    void simultaneousDecisionsCannotApplyTwice() throws Exception {
        long post = post(newUser());
        long report = report("POST", post, newUser());
        String admin = adminToken();
        var one = CompletableFuture.supplyAsync(() -> decide(report, admin));
        var two = CompletableFuture.supplyAsync(() -> decide(report, admin));
        assertThat(List.of(one.get(), two.get())).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit WHERE report_id = ?", Long.class, report)).isEqualTo(1L);
    }

    private int decide(long id, String admin) {
        try { return call("PUT", "/admin/reports/" + id, Map.of("action", "DISMISS", "note", "검토"), admin).status(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void batchCanSuspendAndUnsuspendSameAuthorAcrossDifferentTargets() throws Exception {
        String author = newUser();
        String reader = newUser();
        String admin = adminToken();
        long authorId = userId(author);
        long first = report("POST", post(author), reader);
        long second = report("POST", post(author), reader);
        var body = Map.of("reportIds", List.of(first, second), "action", "SUSPEND", "note", "반복 도배 검토");
        assertThat(call("POST", "/admin/reports/batch", body, admin).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_report WHERE id IN (?, ?) AND status = 'RESOLVED'",
                Long.class, first, second)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ? AND type = 'MODERATION'",
                Long.class, authorId)).isEqualTo(1L);
        assertThat(call("POST", "/admin/reports/batch", Map.of("reportIds", List.of(first, second),
                "action", "UNSUSPEND", "note", "재검토 해제"), admin).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT suspended_at IS NULL FROM app_user WHERE id = ?", Boolean.class, authorId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE user_id = ? AND type = 'MODERATION'",
                Long.class, authorId)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit WHERE report_id IN (?, ?)",
                Long.class, first, second)).isEqualTo(4L);
        // 다른 요청에서 이미 해제된 상태는 여전히 충돌이어야 합니다.
        assertThat(call("POST", "/admin/reports/batch", Map.of("reportIds", List.of(first, second),
                "action", "UNSUSPEND", "note", "중복 요청"), admin).status()).isEqualTo(409);
    }
}
