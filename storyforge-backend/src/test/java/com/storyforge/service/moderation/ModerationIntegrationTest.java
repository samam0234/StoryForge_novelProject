package com.storyforge.service.moderation;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 신고, 관리자 조치, 차단, 계정 정지, 이용약관, 알림 (docs/plan/moderation.md) */
class ModerationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private long published(String token, String title) throws Exception {
        long project = projectAtOutlineConfirmed(token, 1);
        generateChapter(project, 1, token);
        call("PUT", "/chapters/" + chapterId(project, 1, token) + "/confirm", null, token);
        Map<String, Object> body = new HashMap<>();
        body.put("kind", "SHORT");
        body.put("title", title);
        body.put("visibility", "PUBLIC");
        body.put("clonePolicy", "OPEN");
        body.put("downloadable", true);
        Response r = call("POST", "/projects/" + project + "/publish", body, token);
        assertThat(r.status()).isEqualTo(201);
        return r.json().get("id").asLong();
    }

    private long report(String type, long id, String token) throws Exception {
        Response r = call("POST", "/reports", Map.of("targetType", type, "targetId", id, "reason", "ABUSE", "detail", "욕설"), token);
        assertThat(r.status()).isEqualTo(201);
        return r.json().get("id").asLong();
    }

    private long me(String token) throws Exception {
        return call("GET", "/auth/me", null, token).json().get("id").asLong();
    }

    private List<String> notifications(String token) throws Exception {
        List<String> out = new ArrayList<>();
        call("GET", "/me/notifications?size=50", null, token).json().get("items").forEach(n -> out.add(n.get("message").asString()));
        return out;
    }

    private static String q(String text) {
        return java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ───────────── 신고 ─────────────

    @Test
    void onlyAdminsSeeReportsAndReportsAreDeduplicated() throws Exception {
        String author = newUser();
        String reader = newUser();
        long work = published(author, "신고 대상");

        assertThat(call("POST", "/reports", Map.of("targetType", "WORK", "targetId", work, "reason", "SPAM"), null).status()).isEqualTo(401);
        assertThat(call("POST", "/reports", Map.of("targetType", "WORK", "targetId", work, "reason", "SPAM"), author).status()).isEqualTo(409);
        assertThat(call("POST", "/reports", Map.of("targetType", "WORK", "targetId", 99999999, "reason", "SPAM"), reader).status()).isEqualTo(404);

        long first = report("WORK", work, reader);
        assertThat(report("WORK", work, reader)).isEqualTo(first);

        assertThat(call("GET", "/admin/reports", null, null).status()).isEqualTo(401);
        assertThat(call("GET", "/admin/reports", null, reader).status()).isEqualTo(403);
        assertThat(call("PUT", "/admin/reports/" + first, Map.of("action", "HIDE"), reader).status()).isEqualTo(403);

        JsonNode queue = call("GET", "/admin/reports?size=50", null, adminToken()).json();
        JsonNode row = null;
        for (JsonNode n : queue.get("items")) {
            if (n.get("id").asLong() == first) {
                row = n;
            }
        }
        assertThat(row).isNotNull();
        assertThat(row.get("target").get("title").asString()).isEqualTo("신고 대상");
        assertThat(row.get("openReports").asInt()).isEqualTo(1);
    }

    @Test
    void hiddenWorkDisappearsForOthersAndCanBeRestored() throws Exception {
        String author = newUser();
        String reader = newUser();
        String admin = adminToken();
        String marker = "숨김" + UUID.randomUUID();
        long work = published(author, marker);
        long reportId = report("WORK", work, reader);

        assertThat(call("PUT", "/admin/reports/" + reportId, Map.of("action", "HIDE", "note", "정책 위반"), admin).status()).isEqualTo(200);

        assertThat(call("GET", "/works?kind=SHORT&q=" + q(marker), null, null).json().get("total").asInt()).isZero();
        assertThat(call("GET", "/works/" + work, null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/works/" + work, null, reader).status()).isEqualTo(404);
        assertThat(call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), reader).status()).isEqualTo(404);
        assertThat(call("GET", "/users/" + me(author) + "/works", null, null).json()).isEmpty();
        assertThat(call("GET", "/issues", null, null).body()).doesNotContain(marker);

        JsonNode own = call("GET", "/works/" + work, null, author).json();
        assertThat(own.get("card").get("hidden").asBoolean()).isTrue();
        assertThat(call("GET", "/works/" + work, null, admin).status()).isEqualTo(200);
        assertThat(notifications(author)).anyMatch(m -> m.contains("숨겨졌습니다") && m.contains("정책 위반"));
        assertThat(notifications(reader)).contains("신고하신 내용을 처리했습니다.");
        // 작성자에게 신고자를 알리지 않습니다.
        assertThat(String.join(" ", notifications(author))).doesNotContain("테스터님이 신고");

        assertThat(call("PUT", "/admin/reports/" + reportId, Map.of("action", "RESTORE"), admin).status()).isEqualTo(200);
        assertThat(call("GET", "/works/" + work, null, null).status()).isEqualTo(200);
        assertThat(call("GET", "/works?kind=SHORT&q=" + q(marker), null, null).json().get("total").asInt()).isEqualTo(1);
    }

    @Test
    void hiddenCommentAndPostAreMasked() throws Exception {
        String author = newUser();
        String reader = newUser();
        String admin = adminToken();
        long work = published(author, "댓글 숨김");
        long comment = call("POST", "/works/" + work + "/comments", Map.of("content", "나쁜 말"), reader).json().get("id").asLong();
        call("PUT", "/admin/reports/" + report("COMMENT", comment, author), Map.of("action", "HIDE"), admin);

        JsonNode view = call("GET", "/works/" + work + "/comments", null, null).json().get(0);
        assertThat(view.get("hidden").asBoolean()).isTrue();
        assertThat(view.get("content").asString()).isEqualTo("관리자가 숨긴 댓글입니다.");
        assertThat(call("GET", "/works/" + work + "/comments", null, reader).body()).doesNotContain("나쁜 말");
        assertThat(call("PUT", "/comments/" + comment, Map.of("content", "고침"), reader).status()).isEqualTo(404);

        String marker = "라운지" + UUID.randomUUID();
        long post = call("POST", "/posts", Map.of("category", "FREE", "title", marker, "content", "내용"), reader).json().get("id").asLong();
        call("PUT", "/admin/reports/" + report("POST", post, author), Map.of("action", "HIDE"), admin);
        assertThat(call("GET", "/posts?q=" + q(marker), null, null).json().get("total").asInt()).isZero();
        assertThat(call("GET", "/posts/" + post, null, author).status()).isEqualTo(404);
        assertThat(call("GET", "/posts/" + post, null, reader).json().get("hidden").asBoolean()).isTrue();
    }

    @Test
    void dismissClosesEveryOpenReportWithoutHiding() throws Exception {
        String author = newUser();
        String one = newUser();
        String two = newUser();
        long work = published(author, "기각");
        long first = report("WORK", work, one);
        report("WORK", work, two);

        JsonNode result = call("PUT", "/admin/reports/" + first, Map.of("action", "DISMISS"), adminToken()).json();
        assertThat(result.get("status").asString()).isEqualTo("DISMISSED");
        assertThat(result.get("openReports").asInt()).isZero();
        assertThat(call("GET", "/works/" + work, null, null).status()).isEqualTo(200);
        assertThat(notifications(two)).contains("신고하신 내용을 검토했으며 조치하지 않았습니다.");
        // 처리된 뒤에는 다시 신고할 수 있습니다.
        assertThat(report("WORK", work, one)).isNotEqualTo(first);
    }

    // ───────────── 계정 정지 ─────────────

    @Test
    void suspendedAccountCanReadButNotWriteOrLogin() throws Exception {
        String email = "bad" + UUID.randomUUID() + "@example.com";
        String bad = call("POST", "/auth/register", Map.of("email", email, "acceptTerms", true, "password", "password123", "displayName", "문제"), null)
                .json().get("accessToken").asString();
        String reader = newUser();
        String admin = adminToken();
        long reportId = report("USER", me(bad), reader);

        assertThat(call("PUT", "/admin/reports/" + reportId, Map.of("action", "HIDE"), admin).status()).isEqualTo(409);
        assertThat(call("PUT", "/admin/reports/" + reportId, Map.of("action", "SUSPEND", "note", "도배"), admin).status()).isEqualTo(200);

        Response write = call("POST", "/projects", Map.of("title", "x"), bad);
        assertThat(write.status()).isEqualTo(403);
        assertThat(write.json().get("detail").asString()).contains("도배");
        assertThat(call("GET", "/projects", null, bad).status()).isEqualTo(200);
        assertThat(call("POST", "/auth/login", Map.of("email", email, "password", "password123"), null).status()).isEqualTo(403);
        assertThat(call("GET", "/users/" + me(bad), null, null).json().get("suspended").asBoolean()).isTrue();

        call("PUT", "/admin/reports/" + reportId, Map.of("action", "UNSUSPEND"), admin);
        assertThat(call("POST", "/projects", Map.of("title", "x"), bad).status()).isEqualTo(201);
        assertThat(call("POST", "/auth/login", Map.of("email", email, "password", "password123"), null).status()).isEqualTo(200);
    }

    @Test
    void adminCannotBeSuspended() throws Exception {
        String reader = newUser();
        String admin = adminToken();
        long reportId = report("USER", me(admin), reader);
        assertThat(call("PUT", "/admin/reports/" + reportId, Map.of("action", "SUSPEND"), admin).status()).isEqualTo(409);
    }

    @Test
    void deletedAccountTokenCannotWrite() throws Exception {
        String token = newUser();
        call("POST", "/me/delete", Map.of("password", "password123"), token);
        assertThat(call("POST", "/projects", Map.of("title", "x"), token).status()).isEqualTo(401);
    }

    // ───────────── 차단 ─────────────

    @Test
    void blockHidesContentAndStopsInteraction() throws Exception {
        String me = newUser();
        String troll = newUser();
        long trollId = me(troll);
        String marker = "차단" + UUID.randomUUID();
        long trollWork = published(troll, marker);
        long myWork = published(me, "내 글");
        long comment = call("POST", "/works/" + myWork + "/comments", Map.of("content", "첫 댓글"), troll).json().get("id").asLong();
        call("POST", "/works/" + myWork + "/comments", Map.of("content", "내 답글", "parentId", comment), me);

        assertThat(call("PUT", "/users/" + me(me) + "/block", null, me).status()).isEqualTo(409);
        assertThat(call("PUT", "/users/999999999/block", null, me).status()).isEqualTo(404);
        assertThat(call("PUT", "/users/" + trollId + "/block", null, me).status()).isEqualTo(204);
        assertThat(call("PUT", "/users/" + trollId + "/block", null, me).status()).isEqualTo(204);
        assertThat(call("GET", "/users/" + trollId, null, me).json().get("blockedByMe").asBoolean()).isTrue();
        assertThat(call("GET", "/me/blocks", null, me).json().get(0).get("id").asLong()).isEqualTo(trollId);

        assertThat(call("GET", "/works?kind=SHORT&q=" + q(marker), null, me).json().get("total").asInt()).isZero();
        assertThat(call("GET", "/works?kind=SHORT&q=" + q(marker), null, null).json().get("total").asInt()).isEqualTo(1);
        assertThat(call("GET", "/works/" + myWork + "/comments", null, me).json()).isEmpty();
        assertThat(call("GET", "/works/" + myWork + "/comments", null, null).json()).hasSize(2);

        assertThat(call("POST", "/works/" + myWork + "/comments", Map.of("content", "또"), troll).status()).isEqualTo(409);
        assertThat(call("POST", "/packages", Map.of("title", "p", "packageType", "CUSTOM", "visibility", "PUBLIC",
                "memberWorkIds", List.of(trollWork)), troll).status()).isEqualTo(201);
        long pkg = call("GET", "/me/works", null, troll).json().get(0).get("id").asLong();
        assertThat(call("POST", "/packages/" + pkg + "/items", Map.of("memberWorkId", myWork, "role", "MAIN"), troll).status()).isEqualTo(409);

        assertThat(call("DELETE", "/users/" + trollId + "/block", null, me).status()).isEqualTo(204);
        assertThat(call("POST", "/works/" + myWork + "/comments", Map.of("content", "다시"), troll).status()).isEqualTo(201);
    }

    // ───────────── 이용약관 ─────────────

    @Test
    void termsAreRequiredAtSignupAndForPublishing() throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("email", "t" + UUID.randomUUID() + "@example.com", "password", "password123", "displayName", "가"));
        assertThat(call("POST", "/auth/register", body, null).status()).isEqualTo(400);
        body.put("acceptTerms", false);
        assertThat(call("POST", "/auth/register", body, null).status()).isEqualTo(400);
        body.put("acceptTerms", true);
        Response ok = call("POST", "/auth/register", body, null);
        assertThat(ok.status()).isEqualTo(201);
        assertThat(ok.json().get("user").get("termsAccepted").asBoolean()).isTrue();

        // 약관 도입 전 가입자나 새 버전 미동의자는 동의해야 게시·글쓰기를 할 수 있습니다.
        String token = ok.json().get("accessToken").asString();
        long user = ok.json().get("user").get("id").asLong();
        jdbc.update("UPDATE app_user SET terms_version = NULL WHERE id = ?", user);
        assertThat(call("GET", "/auth/me", null, token).json().get("termsAccepted").asBoolean()).isFalse();
        assertThat(call("POST", "/posts", Map.of("category", "FREE", "title", "t", "content", "c"), token).status()).isEqualTo(409);
        assertThat(call("PUT", "/me/terms", null, token).json().get("termsAccepted").asBoolean()).isTrue();
        assertThat(call("POST", "/posts", Map.of("category", "FREE", "title", "t", "content", "c"), token).status()).isEqualTo(201);
    }

    @Test
    void adminFlagComesFromConfiguration() throws Exception {
        assertThat(call("GET", "/auth/me", null, adminToken()).json().get("admin").asBoolean()).isTrue();
        assertThat(call("GET", "/auth/me", null, newUser()).json().get("admin").asBoolean()).isFalse();
    }

    // ───────────── 알림 ─────────────

    @Test
    void notificationsReachAuthorAndNotSelfOrBlocked() throws Exception {
        String author = newUser();
        String fan = newUser();
        long work = published(author, "알림 작품");

        call("POST", "/works/" + work + "/comments", Map.of("content", "비밀스러운 감상"), fan);
        long own = call("POST", "/works/" + work + "/comments", Map.of("content", "내 댓글"), author).json().get("id").asLong();
        call("POST", "/works/" + work + "/comments", Map.of("content", "답글", "parentId", own), fan);
        call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), fan);
        call("POST", "/works/" + work + "/shares", Map.of("channel", "LINK"), fan);

        List<String> got = notifications(author);
        assertThat(got).anyMatch(m -> m.contains("댓글을 남겼습니다"));
        assertThat(got).anyMatch(m -> m.contains("Clone했습니다"));
        assertThat(got).anyMatch(m -> m.contains("공유했습니다"));
        assertThat(got).anyMatch(m -> m.contains("업적을 달성했습니다"));
        // 댓글 내용은 넣지 않습니다.
        assertThat(String.join(" ", got)).doesNotContain("비밀스러운 감상");
        // 답글을 다는 사람 자신에게는 알리지 않습니다.
        assertThat(notifications(fan)).noneMatch(m -> m.contains("답글"));

        long unread = call("GET", "/me/notifications/unread-count", null, author).json().get("count").asLong();
        assertThat(unread).isGreaterThan(3);
        long first = call("GET", "/me/notifications", null, author).json().get("items").get(0).get("id").asLong();
        assertThat(call("PUT", "/me/notifications/" + first + "/read", null, fan).status()).isEqualTo(404);
        assertThat(call("PUT", "/me/notifications/" + first + "/read", null, author).json().get("read").asBoolean()).isTrue();
        assertThat(call("GET", "/me/notifications/unread-count", null, author).json().get("count").asLong()).isEqualTo(unread - 1);
        assertThat(call("PUT", "/me/notifications/read-all", null, author).status()).isEqualTo(204);
        assertThat(call("GET", "/me/notifications/unread-count", null, author).json().get("count").asLong()).isZero();
        assertThat(call("GET", "/me/notifications", null, null).status()).isEqualTo(401);

        // 차단한 사람의 행동은 알리지 않습니다. (차단하면 댓글도 막히므로 공유로 확인)
        call("PUT", "/users/" + me(fan) + "/block", null, author);
        call("POST", "/works/" + work + "/shares", Map.of("channel", "BLOG"), fan);
        assertThat(call("GET", "/me/notifications/unread-count", null, author).json().get("count").asLong()).isZero();
    }

    @Test
    void milestoneNotifiesOnlyOnce() throws Exception {
        String author = newUser();
        String fan = newUser();
        long work = published(author, "달성");
        for (int i = 0; i < 9; i++) {
            Long user = jdbc.queryForObject("INSERT INTO app_user (email, password_hash, display_name, created_at) VALUES (?, 'x', '팬', now()) RETURNING id",
                    Long.class, "fan" + UUID.randomUUID() + "@example.com");
            jdbc.update("INSERT INTO work_clone (work_id, user_id, mode, revision, created_at) VALUES (?, ?, 'WORLD', 1, now())", work, user);
        }
        jdbc.update("UPDATE work SET clone_count = 9 WHERE id = ?", work);
        call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), fan);
        call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), fan);
        assertThat(notifications(author).stream().filter(m -> m.contains("누적 Clone 10회")).count()).isEqualTo(1);
    }
}
