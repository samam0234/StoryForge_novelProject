package com.storyforge.service.community;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationIntegrationTest extends IntegrationTestBase {

    @DynamicPropertySource
    static void moderation(DynamicPropertyRegistry registry) {
        registry.add("storyforge.moderation.blocklist", () -> "금지어");
        registry.add("storyforge.admin.emails", () -> "admin-moderation@example.com");
        registry.add("storyforge.llm.provider", () -> "anthropic");
    }

    @Test
    void reportHideBlockAndExternalNotice() throws Exception {
        String author = newUser();
        String reader = newUser();
        long postId = call("POST", "/posts", Map.of("category", "FREE", "title", "공개 글", "content", "본문"), author)
                .json().get("id").asLong();

        assertThat(call("POST", "/reports", Map.of("targetType", "POST", "targetId", postId, "reason", "SPAM"), null).status())
                .isEqualTo(401);
        assertThat(call("POST", "/reports", Map.of("targetType", "POST", "targetId", postId, "reason", "SPAM"), author).status())
                .isEqualTo(409);
        Response reported = call("POST", "/reports",
                Map.of("targetType", "POST", "targetId", postId, "reason", "ABUSE", "detail", "반복 도배"), reader);
        assertThat(reported.status()).isEqualTo(201);
        assertThat(call("GET", "/admin/reports", null, reader).status()).isEqualTo(404);

        String adminEmail = "admin-moderation@example.com";
        String admin = call("POST", "/auth/register", Map.of(
                "email", adminEmail, "password", "password123", "displayName", "관리", "termsAccepted", true), null)
                .json().get("accessToken").asString();
        assertThat(call("GET", "/auth/me", null, admin).json().get("role").asString()).isEqualTo("ADMIN");
        assertThat(call("GET", "/admin/reports", null, admin).json()).isNotEmpty();
        long reportId = reported.json().get("id").asLong();
        assertThat(call("POST", "/admin/reports/" + reportId + "/hide", null, admin).status()).isEqualTo(200);
        assertThat(call("GET", "/posts/" + postId, null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/posts/" + postId, null, author).status()).isEqualTo(200);

        long second = call("POST", "/posts", Map.of("category", "FREE", "title", "다른 글", "content", "아직 보임"), author)
                .json().get("id").asLong();
        long authorId = call("GET", "/auth/me", null, author).json().get("id").asLong();
        assertThat(call("POST", "/blocks", Map.of("userId", authorId), reader).status()).isEqualTo(204);
        assertThat(call("GET", "/posts/" + second, null, reader).status()).isEqualTo(404);
        assertThat(call("GET", "/posts/" + second, null, null).status()).isEqualTo(200);
        assertThat(call("GET", "/users/" + authorId, null, reader).json().get("blocked").asBoolean()).isTrue();
        assertThat(call("DELETE", "/blocks/" + authorId, null, reader).status()).isEqualTo(204);

        assertThat(call("POST", "/posts", Map.of("category", "FREE", "title", "금지어 포함", "content", "안 됨"), reader).status())
                .isEqualTo(409);
        Response notice = call("GET", "/llm-notice", null, null);
        assertThat(notice.status()).isEqualTo(200);
        assertThat(notice.json().get("external").asBoolean()).isTrue();
        assertThat(notice.json().get("notice").asString()).contains("외부 언어 모델");
    }

    @Test
    void secondOpenReportIsRejected() throws Exception {
        String author = newUser();
        String reader = newUser();
        long postId = call("POST", "/posts", Map.of("category", "TIP", "title", "팁 " + UUID.randomUUID(), "content", "내용"), author)
                .json().get("id").asLong();
        assertThat(call("POST", "/reports", Map.of("targetType", "POST", "targetId", postId, "reason", "OTHER"), reader).status())
                .isEqualTo(201);
        assertThat(call("POST", "/reports", Map.of("targetType", "POST", "targetId", postId, "reason", "OTHER"), reader).status())
                .isEqualTo(409);
    }
}
