package com.storyforge.service.orchestration;

import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectRepository;
import com.storyforge.domain.project.ProjectStatus;
import com.storyforge.support.IntegrationTestBase;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.storyforge.support.StubAiServer.ChapterMode;

import static org.assertj.core.api.Assertions.assertThat;

class StabilityIntegrationTest extends IntegrationTestBase {

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void streamWithoutDoneRevertsAndDoesNotSavePartialChapter(int number) throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        if (number == 2) {
            generateChapter(project, 1, token);
            call("PUT", "/chapters/" + chapterId(project, 1, token) + "/confirm", null, token);
        }
        AI.chapterMode(ChapterMode.MISSING_DONE);

        Response response = generateChapter(project, number, token);

        assertThat(response.body()).contains("event:error").doesNotContain("event:done");
        assertThat(status(project, token)).isEqualTo(number == 1 ? "OUTLINE_CONFIRMED" : "CHAPTER_REVIEW");
        assertThat(call("GET", "/projects/" + project + "/chapters", null, token).json().size()).isEqualTo(number - 1);
        AI.chapterMode(ChapterMode.OK);
        assertThat(generateChapter(project, number, token).body()).contains("event:done");
    }

    @Autowired
    private PipelineRecovery recovery;
    @Autowired
    private ProjectRepository projects;
    @Autowired
    private TransactionTemplate tx;

    private void forceStatus(long id, ProjectStatus status) {
        tx.executeWithoutResult(s -> {
            Project p = projects.findById(id).orElseThrow();
            p.changeStatus(status);
        });
    }

    @Test
    void stuckFirstChapterRecoversToOutlineConfirmed() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        forceStatus(project, ProjectStatus.CHAPTER_GENERATING);

        assertThat(recovery.recoverStuckProjects()).isGreaterThanOrEqualTo(1);
        assertThat(status(project, token)).isEqualTo("OUTLINE_CONFIRMED");
        assertThat(generateChapter(project, 1, token).status()).as("복구 후 다시 생성 가능").isEqualTo(200);
    }

    @Test
    void stuckLaterChapterRecoversToReview() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        generateChapter(project, 1, token);
        call("PUT", "/chapters/" + chapterId(project, 1, token) + "/confirm", null, token);
        forceStatus(project, ProjectStatus.CHAPTER_GENERATING);

        recovery.recoverStuckProjects();
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndStopsCallingAi() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();
        AI.failSync(true);

        for (int i = 0; i < 5; i++) {
            assertThat(call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token).status()).isEqualTo(502);
        }
        assertThat(aiCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int callsBefore = AI.syncCalls();
        Response blocked = call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token);
        assertThat(blocked.status()).isEqualTo(502);
        assertThat(blocked.body()).doesNotContain("Circuit");
        assertThat(AI.syncCalls()).as("차단 중에는 AI를 호출하지 않음").isEqualTo(callsBefore);
    }

    @Test
    void concurrentGenerationOfSameChapterRunsOnlyOnce() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        AI.chapterDelayMillis(500);

        List<CompletableFuture<Response>> requests = List.of(
                CompletableFuture.supplyAsync(() -> generate(project, token)),
                CompletableFuture.supplyAsync(() -> generate(project, token)));
        List<Integer> statuses = requests.stream().map(f -> f.join().status()).sorted().toList();

        assertThat(statuses).containsExactly(200, 409);
        assertThat(AI.chapterRequests).hasSize(1);
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW");
    }

    @Test
    void connectionFailureGivesBadGatewayNotServerError() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();
        AI.stop();
        try {
            assertThat(call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token).status()).isEqualTo(502);
        } finally {
            AI.restart();
        }
    }

    private Response generate(long project, String token) {
        try {
            return generateChapter(project, 1, token);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
