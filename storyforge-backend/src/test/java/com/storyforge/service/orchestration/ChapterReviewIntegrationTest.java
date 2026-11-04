package com.storyforge.service.orchestration;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 생성 뒤 고친 원고는 승인할 때 다시 요약·검증합니다 (설정 어긋남 방지). */
class ChapterReviewIntegrationTest extends IntegrationTestBase {

    private long generated(long project, String token) throws Exception {
        generateChapter(project, 1, token);
        return chapterId(project, 1, token);
    }

    private void edit(long chapter, String content, String token) throws Exception {
        call("PUT", "/chapters/" + chapter, Map.of("title", "제목", "content", content), token);
    }

    @Test
    void uneditedChapterIsNotReviewedAgain() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        long chapter = generated(project, token);

        assertThat(call("GET", "/chapters/" + chapter, null, token).json().get("needsReview").asBoolean()).isFalse();
        // 내용을 바꾸지 않은 저장도 다시 검증하지 않습니다 (불필요한 비용 방지).
        call("PUT", "/chapters/" + chapter, Map.of("title", "새 제목", "content", "첫 문단 이어서"), token);
        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, token).status()).isEqualTo(200);
        assertThat(AI.reviewRequests).isEmpty();
    }

    @Test
    void editedChapterGetsNewSummaryThatReachesNextChapterContext() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        long chapter = generated(project, token);
        edit(chapter, "사람이 크게 고친 본문", token);
        assertThat(call("GET", "/chapters/" + chapter, null, token).json().get("needsReview").asBoolean()).isTrue();

        Response confirmed = call("PUT", "/chapters/" + chapter + "/confirm", null, token);
        assertThat(confirmed.status()).isEqualTo(200);
        assertThat(AI.reviewRequests).hasSize(1);
        JsonNode request = JSON.readTree(AI.reviewRequests.get(0));
        assertThat(request.get("content").asString()).isEqualTo("사람이 크게 고친 본문");
        assertThat(request.get("context").get("chapterNumber").asInt()).isEqualTo(1);

        JsonNode body = confirmed.json();
        assertThat(body.get("status").asString()).isEqualTo("CONFIRMED");
        assertThat(body.get("needsReview").asBoolean()).isFalse();
        assertThat(body.get("summary").asString()).isEqualTo("수정본 요약");
        assertThat(body.get("continuityReport").get("score").asInt()).isEqualTo(100);

        generateChapter(project, 2, token);
        assertThat(AI.chapterRequests.get(AI.chapterRequests.size() - 1)).contains("수정본 요약");
    }

    @Test
    void highIssueStopsApprovalOnceAndShowsReport() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        long chapter = generated(project, token);
        edit(chapter, "설정과 어긋나게 고친 본문", token);
        AI.reviewFindsHighIssue(true);

        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, token).status()).isEqualTo(409);
        JsonNode after = call("GET", "/chapters/" + chapter, null, token).json();
        assertThat(after.get("status").asString()).isEqualTo("DRAFT");
        assertThat(after.get("continuityReport").get("issues").get(0).get("description").asString()).isEqualTo("수정본 어긋남");
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");

        // 리포트를 본 뒤 다시 승인하면 사용자 판단으로 진행합니다. 다시 호출하지 않습니다.
        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, token).status()).isEqualTo(200);
        assertThat(AI.reviewRequests).hasSize(1);
        assertThat(status(project, token)).isEqualTo("COMPLETED");
    }

    @Test
    void reviewFailureDoesNotApprove() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        long chapter = generated(project, token);
        edit(chapter, "고친 본문", token);
        AI.failReview(true);

        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, token).status()).isEqualTo(502);
        JsonNode after = call("GET", "/chapters/" + chapter, null, token).json();
        assertThat(after.get("status").asString()).isEqualTo("DRAFT");
        assertThat(after.get("needsReview").asBoolean()).isTrue();
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");
    }

    @Test
    void regeneratingClearsReviewFlag() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        long chapter = generated(project, token);
        edit(chapter, "고친 본문", token);

        generateChapter(project, 1, token);
        assertThat(call("GET", "/chapters/" + chapter, null, token).json().get("needsReview").asBoolean()).isFalse();
        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, token).status()).isEqualTo(200);
        assertThat(AI.reviewRequests).isEmpty();
    }
}
