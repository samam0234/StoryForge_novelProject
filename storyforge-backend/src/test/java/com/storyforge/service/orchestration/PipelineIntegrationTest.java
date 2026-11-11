package com.storyforge.service.orchestration;

import com.storyforge.support.IntegrationTestBase;
import com.storyforge.support.StubAiServer.ChapterMode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineIntegrationTest extends IntegrationTestBase {

    @Test
    void fullPipelineFromBibleToCompleted() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        assertThat(status(project, token)).isEqualTo("OUTLINE_CONFIRMED");

        Response stream = generateChapter(project, 1, token);
        assertThat(stream.status()).isEqualTo(200);
        assertThat(stream.body()).contains("event:token").contains("event:done");
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");

        JsonNode draft = call("GET", "/chapters/" + chapterId(project, 1, token), null, token).json();
        assertThat(draft.get("status").asString()).isEqualTo("DRAFT");
        assertThat(draft.get("continuityReport").get("score").asInt()).isEqualTo(70);
        assertThat(draft.get("continuityReport").get("issues").get(0).get("severity").asString()).isEqualTo("high");

        assertThat(call("PUT", "/chapters/" + draft.get("id").asLong() + "/confirm", null, token).status()).isEqualTo(200);
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");

        generateChapter(project, 2, token);
        call("PUT", "/chapters/" + chapterId(project, 2, token) + "/confirm", null, token);
        assertThat(status(project, token)).isEqualTo("COMPLETED");
    }

    @Test
    void chapterContextContainsBibleOutlineAndPreviousSummary() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);

        generateChapter(project, 1, token);
        call("PUT", "/chapters/" + chapterId(project, 1, token) + "/confirm", null, token);
        generateChapter(project, 2, token);

        JsonNode context = JSON.readTree(AI.chapterRequests.get(1));
        assertThat(context.get("project").get("creativeBrief").asString()).isEqualTo("아이디어");
        assertThat(context.get("project").get("form").asString()).isEqualTo("LONG");
        assertThat(context.get("bible").get("worldSetting").asString()).isEqualTo("세계");
        assertThat(context.get("bible").get("characters").get(0).get("name").asString()).isEqualTo("한서윤");
        assertThat(context.get("outlineChapter").get("title").asString()).isEqualTo("2장");
        assertThat(context.get("previousChapters").get(0).get("summary").asString()).isEqualTo("요약");
    }

    @Test
    void cannotSkipStages() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();

        assertThat(call("POST", "/projects/" + project + "/outline/generate", Map.of("chapterCount", 2), token).status()).isEqualTo(409);
        assertThat(generateChapter(project, 1, token).status()).isEqualTo(409);
        assertThat(call("PUT", "/projects/" + project + "/bible/confirm", null, token).status()).isEqualTo(404);
    }

    @Test
    void cannotSkipChapterReview() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 3);

        assertThat(generateChapter(project, 2, token).status()).isEqualTo(409);
        generateChapter(project, 1, token);
        assertThat(generateChapter(project, 2, token).status()).as("1장 승인 전 2장 생성").isEqualTo(409);
    }

    @Test
    void draftCanBeEditedAndRegeneratedButConfirmedCannot() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        generateChapter(project, 1, token);
        long chapter = chapterId(project, 1, token);

        assertThat(call("PUT", "/chapters/" + chapter, Map.of("title", "새 제목", "content", "**수정**"), token).status()).isEqualTo(200);
        assertThat(call("GET", "/chapters/" + chapter, null, token).json().get("content").asString()).isEqualTo("**수정**");

        assertThat(generateChapter(project, 1, token).status()).as("재생성").isEqualTo(200);
        assertThat(chapterId(project, 1, token)).isEqualTo(chapter);
        assertThat(call("GET", "/chapters/" + chapter, null, token).json().get("content").asString()).isEqualTo("첫 문단 이어서");

        call("PUT", "/chapters/" + chapter + "/confirm", null, token);
        assertThat(call("PUT", "/chapters/" + chapter, Map.of("title", "x", "content", "y"), token).status()).isEqualTo(409);
        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, token).status()).isEqualTo(409);
        assertThat(generateChapter(project, 1, token).status()).isEqualTo(409);
    }

    @Test
    void firstChapterFailureRevertsToOutlineConfirmed() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);

        for (ChapterMode mode : List.of(ChapterMode.SSE_ERROR, ChapterMode.HTTP_500)) {
            AI.chapterMode(mode);
            Response stream = generateChapter(project, 1, token);
            assertThat(stream.body()).as(mode.name()).contains("event:error").doesNotContain("event:done");
            assertThat(status(project, token)).as(mode.name()).isEqualTo("OUTLINE_CONFIRMED");
        }
        assertThat(call("GET", "/projects/" + project + "/chapters", null, token).json().size()).isZero();
    }

    @Test
    void laterChapterFailureRevertsToReview() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        generateChapter(project, 1, token);
        call("PUT", "/chapters/" + chapterId(project, 1, token) + "/confirm", null, token);

        AI.chapterMode(ChapterMode.SSE_ERROR);
        generateChapter(project, 2, token);
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");
    }

    @Test
    void aiFailureOnBibleGivesBadGatewayWithoutInternalDetails() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();

        AI.failSync(true);
        Response r = call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token);
        assertThat(r.status()).isEqualTo(502);
        assertThat(r.body()).doesNotContain("127.0.0.1").doesNotContain("/internal/");
        assertThat(status(project, token)).isEqualTo("BIBLE_DRAFT");
    }

    @Test
    void bibleCannotBeEditedAfterConfirm() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        Map<String, Object> body = Map.of("worldSetting", "x", "theme", "y", "tone", "z", "characters", List.of());
        assertThat(call("PUT", "/projects/" + project + "/bible", body, token).status()).isEqualTo(409);
    }

    @Test
    void outlineReorderRenumbersChapters() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();
        call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token);
        call("PUT", "/projects/" + project + "/bible/confirm", null, token);
        JsonNode chapters = call("POST", "/projects/" + project + "/outline/generate", Map.of("chapterCount", 2), token).json().get("chapters");

        Response r = call("PUT", "/projects/" + project + "/outline", Map.of("chapters", List.of(chapters.get(1), chapters.get(0))), token);
        assertThat(r.status()).isEqualTo(200);
        JsonNode saved = r.json().get("chapters");
        assertThat(saved.get(0).get("order").asInt()).isEqualTo(1);
        assertThat(saved.get(0).get("title").asString()).isEqualTo("2장");
    }
}
