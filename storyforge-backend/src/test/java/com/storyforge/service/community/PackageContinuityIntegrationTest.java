package com.storyforge.service.community;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 패키지 전체 Continuity 검사와 원본 변경 가져오기 */
class PackageContinuityIntegrationTest extends IntegrationTestBase {

    private long project(String token) throws Exception {
        long id = projectAtOutlineConfirmed(token, 1);
        generateChapter(id, 1, token);
        call("PUT", "/chapters/" + chapterId(id, 1, token) + "/confirm", null, token);
        return id;
    }

    private Map<String, Object> publish(String title, String policy) {
        Map<String, Object> body = new HashMap<>();
        body.put("kind", "SHORT");
        body.put("title", title);
        body.put("visibility", "PUBLIC");
        body.put("clonePolicy", policy);
        body.put("downloadable", true);
        return body;
    }

    private long work(String token, String title, String policy) throws Exception {
        return call("POST", "/projects/" + project(token) + "/publish", publish(title, policy), token).json().get("id").asLong();
    }

    private long packageOf(String token, List<Long> members) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "묶음");
        body.put("packageType", "UNIVERSE");
        body.put("visibility", "PUBLIC");
        body.put("worldNote", "공용 규칙");
        body.put("memberWorkIds", members);
        return call("POST", "/packages", body, token).json().get("id").asLong();
    }

    @Test
    void ownerRunsCheckAndLatestReportIsKept() throws Exception {
        String owner = newUser();
        long pkg = packageOf(owner, List.of(work(owner, "첫째", "OPEN"), work(owner, "둘째", "OPEN")));

        assertThat(call("GET", "/packages/" + pkg + "/continuity", null, owner).body()).isEmpty();
        JsonNode report = call("POST", "/packages/" + pkg + "/continuity", null, owner).json();
        assertThat(report.get("score").asInt()).isEqualTo(88);
        assertThat(report.get("issues").get(0).get("workTitles").get(1).asString()).isEqualTo("나");

        JsonNode request = JSON.readTree(AI.packageRequests.get(0));
        assertThat(request.get("worldNote").asString()).isEqualTo("공용 규칙");
        assertThat(request.get("works")).hasSize(2);
        assertThat(request.get("works").get(0).get("bible").get("worldSetting").asString()).isEqualTo("세계");
        assertThat(request.get("works").get(0).get("chapters").get(0).get("summary").asString()).isEqualTo("요약");

        assertThat(call("GET", "/packages/" + pkg + "/continuity", null, owner).json().get("score").asInt()).isEqualTo(88);
        String other = newUser();
        assertThat(call("POST", "/packages/" + pkg + "/continuity", null, other).status()).isEqualTo(404);
        assertThat(call("GET", "/packages/" + pkg + "/continuity", null, other).status()).isEqualTo(404);
        assertThat(call("GET", "/packages/" + pkg + "/continuity", null, null).status()).isEqualTo(401);
    }

    @Test
    void closedWorkOfOtherAuthorIsNotSentToAi() throws Exception {
        String owner = newUser();
        String other = newUser();
        long mine = work(owner, "내 작품", "OPEN");
        long second = work(owner, "내 둘째", "OPEN");
        long closed = work(other, "비공개 설정", "CLOSED");
        long pkg = packageOf(owner, List.of(mine, second));
        long item = call("POST", "/packages/" + pkg + "/items", Map.of("memberWorkId", closed, "role", "SIDE"), owner).json().get("id").asLong();
        call("PUT", "/me/package-requests/" + item, Map.of("approve", true), other);

        JsonNode report = call("POST", "/packages/" + pkg + "/continuity", null, owner).json();
        assertThat(report.get("skippedTitles").get(0).asString()).isEqualTo("비공개 설정");
        assertThat(AI.packageRequests.get(0)).doesNotContain("비공개 설정");
    }

    @Test
    void needsAtLeastTwoReadableWorks() throws Exception {
        String owner = newUser();
        long pkg = packageOf(owner, List.of(work(owner, "하나", "OPEN")));
        assertThat(call("POST", "/packages/" + pkg + "/continuity", null, owner).status()).isEqualTo(409);
        assertThat(AI.packageRequests).isEmpty();
    }

    // ───────────── 원본 변경 가져오기 ─────────────

    private long upstreamWork(String author) throws Exception {
        return work(author, "원본", "OPEN");
    }

    private JsonNode row(JsonNode compare, int number) {
        for (JsonNode c : compare.get("chapters")) {
            if (c.get("number").asInt() == number) {
                return c;
            }
        }
        throw new AssertionError("챕터 없음: " + number);
    }

    @Test
    void pullReplacesDraftKeepsHistoryAndRevalidates() throws Exception {
        String author = newUser();
        String reader = newUser();
        long upstream = upstreamWork(author);
        long clone = call("POST", "/works/" + upstream + "/clone", Map.of("mode", "FULL"), reader).json().get("projectId").asLong();
        long chapter = chapterId(clone, 1, reader);
        call("PUT", "/chapters/" + chapter, Map.of("title", "내 제목", "content", "내가 고친 본문"), reader);
        call("PUT", "/works/" + upstream + "/chapters/1", Map.of("title", "새 원본", "content", "원본 새 본문"), author);

        JsonNode before = call("GET", "/projects/" + clone + "/upstream", null, reader).json();
        assertThat(before.get("currentRevision").asInt()).isEqualTo(2);
        assertThat(row(before, 1).get("state").asString()).isEqualTo("CHANGED");

        JsonNode after = call("POST", "/projects/" + clone + "/upstream/chapters/1/pull", null, reader).json();
        assertThat(row(after, 1).get("state").asString()).isEqualTo("SAME");
        JsonNode pulled = call("GET", "/chapters/" + chapter, null, reader).json();
        assertThat(pulled.get("content").asString()).isEqualTo("원본 새 본문");
        assertThat(pulled.get("status").asString()).isEqualTo("DRAFT");
        assertThat(pulled.get("needsReview").asBoolean()).isTrue();

        // 이전 내용은 이력에 남습니다.
        assertThat(call("GET", "/chapters/" + chapter + "/versions", null, reader).body()).contains("내가 고친 본문");

        // 승인 전 다시 검증합니다.
        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, reader).status()).isEqualTo(200);
        assertThat(AI.reviewRequests).hasSize(1);
        assertThat(AI.reviewRequests.get(0)).contains("원본 새 본문");
        // 승인한 뒤에는 덮어쓸 수 없고, 프로젝트가 끝나 검토 단계도 아닙니다.
        assertThat(call("POST", "/projects/" + clone + "/upstream/chapters/1/pull", null, reader).status()).isEqualTo(409);
    }

    @Test
    void pullIsBlockedOutsideFullCloneOrWhenPolicyChanges() throws Exception {
        String author = newUser();
        String reader = newUser();
        long upstream = upstreamWork(author);
        long world = call("POST", "/works/" + upstream + "/clone", Map.of("mode", "WORLD"), reader).json().get("projectId").asLong();
        assertThat(call("POST", "/projects/" + world + "/upstream/chapters/1/pull", null, reader).status()).isEqualTo(409);

        long full = call("POST", "/works/" + upstream + "/clone", Map.of("mode", "FULL"), reader).json().get("projectId").asLong();
        assertThat(call("POST", "/projects/" + full + "/upstream/chapters/1/pull", null, newUser()).status()).isEqualTo(404);
        assertThat(call("POST", "/projects/" + full + "/upstream/chapters/9/pull", null, reader).status()).isEqualTo(404);

        Map<String, Object> meta = publish("원본", "CLOSED");
        call("PUT", "/works/" + upstream, meta, author);
        assertThat(call("POST", "/projects/" + full + "/upstream/chapters/1/pull", null, reader).status()).isEqualTo(409);
    }

    @Test
    void acknowledgeMovesBaseRevisionWithoutChangingManuscript() throws Exception {
        String author = newUser();
        String reader = newUser();
        long upstream = upstreamWork(author);
        long clone = call("POST", "/works/" + upstream + "/clone", Map.of("mode", "FULL"), reader).json().get("projectId").asLong();
        String before = call("GET", "/projects/" + clone + "/chapters", null, reader).json().get(0).get("content").asString();
        call("PUT", "/works/" + upstream + "/chapters/1", Map.of("title", "새", "content", "원본 새 본문"), author);

        JsonNode acked = call("PUT", "/projects/" + clone + "/upstream/acknowledge", null, reader).json();
        assertThat(acked.get("clonedRevision").asInt()).isEqualTo(2);
        assertThat(acked.get("currentRevision").asInt()).isEqualTo(2);
        assertThat(call("GET", "/projects/" + clone + "/chapters", null, reader).json().get(0).get("content").asString()).isEqualTo(before);
    }
}
