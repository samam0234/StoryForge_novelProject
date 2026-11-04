package com.storyforge.service.community;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/** 게시판, Clone, 공유, 패키지포지, 활동·업적, 프로필 (docs/plan/community.md) */
class CommunityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** 챕터를 approved개 승인한 프로젝트 */
    private long projectWithChapters(String token, int total, int approved) throws Exception {
        long id = projectAtOutlineConfirmed(token, total);
        for (int n = 1; n <= approved; n++) {
            generateChapter(id, n, token);
            call("PUT", "/chapters/" + chapterId(id, n, token) + "/confirm", null, token);
        }
        return id;
    }

    private Map<String, Object> publishBody(String kind, String title) {
        Map<String, Object> body = new HashMap<>();
        body.put("kind", kind);
        body.put("title", title);
        body.put("synopsis", "줄거리");
        body.put("genre", "판타지");
        body.put("tags", "약속, 회귀");
        body.put("visibility", "PUBLIC");
        body.put("clonePolicy", "OPEN");
        body.put("downloadable", true);
        return body;
    }

    private long published(String token, String title) throws Exception {
        long project = projectWithChapters(token, 2, 2);
        Response r = call("POST", "/projects/" + project + "/publish", publishBody("LONG", title), token);
        assertThat(r.status()).isEqualTo(201);
        return r.json().get("id").asLong();
    }

    private static long userId(Response me) {
        return me.json().get("id").asLong();
    }

    // ───────────── 게시 ─────────────

    @Test
    void publishRequiresApprovedChaptersAndIncludesOnlyThem() throws Exception {
        String token = newUser();
        long draftOnly = projectAtOutlineConfirmed(token, 2);
        assertThat(call("POST", "/projects/" + draftOnly + "/publish", publishBody("LONG", "없음"), token).status()).isEqualTo(409);

        long project = projectWithChapters(token, 3, 1);
        generateChapter(project, 2, token); // 2장은 검토 중 초안
        Response work = call("POST", "/projects/" + project + "/publish", publishBody("LONG", "일부 게시"), token);
        assertThat(work.status()).isEqualTo(201);
        assertThat(work.json().get("chapterCount").asInt()).isEqualTo(1);
        long id = work.json().get("id").asLong();

        assertThat(call("GET", "/works/" + id + "/chapters/2", null, null).status()).isEqualTo(404);
        assertThat(status(project, token)).isEqualTo("CHAPTER_REVIEW"); // 게시가 파이프라인 상태를 바꾸지 않음
        assertThat(call("POST", "/projects/" + project + "/publish", publishBody("LONG", "다시"), token).status()).isEqualTo(409);
        assertThat(call("POST", "/projects/" + project + "/publish", publishBody("LONG", "남의 것"), newUser()).status()).isEqualTo(404);
    }

    @Test
    void boardListsOnlyPublicWorksAndAnonymousCanRead() throws Exception {
        String token = newUser();
        String marker = "검색" + UUID.randomUUID();
        long open = published(token, marker + " 공개");

        long hiddenProject = projectWithChapters(token, 1, 1);
        Map<String, Object> privateBody = publishBody("SHORT", marker + " 비공개");
        privateBody.put("visibility", "PRIVATE");
        long hidden = call("POST", "/projects/" + hiddenProject + "/publish", privateBody, token).json().get("id").asLong();

        long linkProject = projectWithChapters(token, 1, 1);
        Map<String, Object> linkBody = publishBody("LONG", marker + " 링크");
        linkBody.put("visibility", "UNLISTED");
        long unlisted = call("POST", "/projects/" + linkProject + "/publish", linkBody, token).json().get("id").asLong();

        JsonNode board = call("GET", "/works?kind=LONG&q=" + java.net.URLEncoder.encode(marker, StandardCharsets.UTF_8), null, null).json();
        assertThat(board.get("total").asInt()).isEqualTo(1);
        assertThat(board.get("items").get(0).get("id").asLong()).isEqualTo(open);
        assertThat(board.get("items").get(0).get("author").has("email")).isFalse();
        // 검색어의 %는 글자 그대로입니다.
        assertThat(call("GET", "/works?kind=LONG&q=%25%25%25" + "nomatch", null, null).json().get("total").asInt()).isZero();

        assertThat(call("GET", "/works/" + open, null, null).status()).isEqualTo(200);
        assertThat(call("GET", "/works/" + open + "/chapters/1", null, null).json().get("next").asInt()).isEqualTo(2);
        assertThat(call("GET", "/works/" + unlisted, null, null).status()).isEqualTo(200);
        assertThat(call("GET", "/works/" + hidden, null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/works/" + hidden, null, newUser()).status()).isEqualTo(404);
        assertThat(call("GET", "/works/" + hidden, null, token).status()).isEqualTo(200);

        // 쓰기는 로그인이 필요합니다.
        assertThat(call("POST", "/works/" + open + "/comments", Map.of("content", "x"), null).status()).isEqualTo(401);
        assertThat(call("PUT", "/works/" + open + "/like", null, null).status()).isEqualTo(401);
    }

    @Test
    void workEditsRaiseRevisionAndStayOutOfProject() throws Exception {
        String token = newUser();
        String other = newUser();
        long project = projectWithChapters(token, 1, 1);
        long work = call("POST", "/projects/" + project + "/publish", publishBody("SHORT", "수정"), token).json().get("id").asLong();
        String original = call("GET", "/projects/" + project + "/chapters", null, token).json().get(0).get("content").asString();

        Map<String, Object> edit = Map.of("title", "새 제목", "content", "고친 본문입니다.", "message", "오타 수정");
        assertThat(call("PUT", "/works/" + work + "/chapters/1", edit, other).status()).isEqualTo(404);
        assertThat(call("PUT", "/works/" + work + "/chapters/1", edit, token).status()).isEqualTo(200);

        JsonNode detail = call("GET", "/works/" + work, null, token).json();
        assertThat(detail.get("card").get("revision").asInt()).isEqualTo(2);
        assertThat(detail.get("revisions").get(0).get("message").asString()).isEqualTo("오타 수정");
        assertThat(call("GET", "/projects/" + project + "/chapters", null, token).json().get(0).get("content").asString()).isEqualTo(original);

        // 프로젝트에서 다시 가져오면 게시본이 프로젝트 원고로 돌아갑니다.
        assertThat(call("POST", "/works/" + work + "/sync", Map.of("message", "동기화"), token).json().get("revision").asInt()).isEqualTo(3);
        assertThat(call("GET", "/works/" + work + "/chapters/1", null, null).json().get("content").asString()).isEqualTo(original);

        Map<String, Object> meta = new HashMap<>(publishBody("SHORT", "바뀐 제목"));
        meta.put("downloadable", false);
        assertThat(call("PUT", "/works/" + work, meta, other).status()).isEqualTo(404);
        assertThat(call("PUT", "/works/" + work, meta, token).json().get("title").asString()).isEqualTo("바뀐 제목");

        // 프로젝트를 지워도 게시본은 남습니다.
        call("DELETE", "/projects/" + project, null, token);
        assertThat(call("GET", "/works/" + work, null, null).status()).isEqualTo(200);
        assertThat(call("DELETE", "/works/" + work, null, other).status()).isEqualTo(404);
        assertThat(call("DELETE", "/works/" + work, null, token).status()).isEqualTo(204);
        assertThat(call("GET", "/works/" + work, null, null).status()).isEqualTo(404);
    }

    // ───────────── 작업 그래프 ─────────────

    @Test
    void workGraphSeparatesManualAndAgentWithoutLeakingText() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        generateChapter(project, 1, token);
        long chapter = chapterId(project, 1, token);
        String secret = "비밀문장" + UUID.randomUUID();
        call("PUT", "/chapters/" + chapter, Map.of("title", "제목", "content", secret), token);
        call("PUT", "/chapters/" + chapter + "/confirm", null, token);

        JsonNode own = call("GET", "/projects/" + project + "/activity", null, token).json();
        assertThat(own.get("agent").get("events").asInt()).isEqualTo(3); // Bible, Outline, 챕터
        assertThat(own.get("agent").get("chars").asLong()).isPositive();
        assertThat(own.get("manual").get("chars").asLong()).isPositive();
        assertThat(own.get("days")).hasSize(1);
        assertThat(own.get("days").get(0).get("agent").asInt()).isEqualTo(3);
        assertThat(call("GET", "/projects/" + project + "/activity", null, newUser()).status()).isEqualTo(404);

        long work = call("POST", "/projects/" + project + "/publish", publishBody("SHORT", "그래프"), token).json().get("id").asLong();
        Response graph = call("GET", "/works/" + work + "/activity", null, null);
        assertThat(graph.status()).isEqualTo(200);
        assertThat(graph.body()).doesNotContain(secret);
        List<String> types = new ArrayList<>();
        graph.json().get("events").forEach(e -> types.add(e.get("type").asString()));
        assertThat(types).contains("BIBLE_GENERATED", "CHAPTER_GENERATED", "CHAPTER_EDITED", "CHAPTER_CONFIRMED", "WORK_PUBLISHED");
        graph.json().get("events").forEach(e -> {
            if (e.get("type").asString().equals("CHAPTER_GENERATED")) {
                assertThat(e.get("actor").asString()).isEqualTo("AGENT");
            }
            if (e.get("type").asString().equals("CHAPTER_EDITED")) {
                assertThat(e.get("actor").asString()).isEqualTo("MANUAL");
            }
        });
    }

    // ───────────── Clone ─────────────

    @Test
    void cloneScopesStartAtMatchingPipelineStage() throws Exception {
        String author = newUser();
        String reader = newUser();
        long work = published(author, "원작");

        long world = call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), reader).json().get("projectId").asLong();
        assertThat(status(world, reader)).isEqualTo("BIBLE_DRAFT");
        assertThat(call("GET", "/projects/" + world + "/bible", null, reader).json().get("worldSetting").asString()).isEqualTo("세계");
        assertThat(call("GET", "/projects/" + world + "/outline", null, reader).status()).isEqualTo(404);

        long plot = call("POST", "/works/" + work + "/clone", Map.of("mode", "PLOT"), reader).json().get("projectId").asLong();
        assertThat(status(plot, reader)).isEqualTo("OUTLINE_DRAFT");
        assertThat(call("GET", "/projects/" + plot + "/chapters", null, reader).json()).isEmpty();

        long full = call("POST", "/works/" + work + "/clone", Map.of("mode", "FULL", "title", "내 판본"), reader).json().get("projectId").asLong();
        assertThat(status(full, reader)).isEqualTo("CHAPTER_REVIEW");
        JsonNode chapters = call("GET", "/projects/" + full + "/chapters", null, reader).json();
        assertThat(chapters).hasSize(2);
        chapters.forEach(c -> assertThat(c.get("status").asString()).isEqualTo("DRAFT"));

        // 복제 원고도 앞 장부터 다시 승인합니다 (사용자 검토를 건너뛰지 않음).
        long second = chapterId(full, 2, reader);
        assertThat(call("PUT", "/chapters/" + second + "/confirm", null, reader).status()).isEqualTo(409);
        assertThat(call("PUT", "/chapters/" + chapterId(full, 1, reader) + "/confirm", null, reader).status()).isEqualTo(200);
        call("PUT", "/chapters/" + second + "/confirm", null, reader);
        assertThat(status(full, reader)).isEqualTo("COMPLETED");

        // Clone한 쪽 수정이 원본을 바꾸지 않습니다.
        assertThat(call("GET", "/projects/" + full + "/chapters", null, author).status()).isEqualTo(404);
        assertThat(call("GET", "/works/" + work, null, null).json().get("card").get("cloneCount").asInt()).isEqualTo(3);
    }

    @Test
    void clonePolicyLimitsScopeButNotAuthor() throws Exception {
        String author = newUser();
        String reader = newUser();
        long work = published(author, "제한");
        Map<String, Object> meta = new HashMap<>(publishBody("LONG", "제한"));
        meta.put("clonePolicy", "WORLD_ONLY");
        call("PUT", "/works/" + work, meta, author);

        assertThat(call("POST", "/works/" + work + "/clone", Map.of("mode", "FULL"), reader).status()).isEqualTo(409);
        assertThat(call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), reader).status()).isEqualTo(201);
        JsonNode detail = call("GET", "/works/" + work, null, reader).json();
        assertThat(detail.get("cloneModes")).hasSize(1);
        assertThat(detail.get("outline").isNull()).isTrue();

        meta.put("clonePolicy", "CLOSED");
        call("PUT", "/works/" + work, meta, author);
        assertThat(call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), reader).status()).isEqualTo(409);
        assertThat(call("GET", "/works/" + work, null, reader).json().get("bible").isNull()).isTrue();
        assertThat(call("POST", "/works/" + work + "/clone", Map.of("mode", "FULL"), author).status()).isEqualTo(201);
    }

    @Test
    void cloneNetworkAndUpstreamCompareTrackDerivatives() throws Exception {
        String author = newUser();
        String reader = newUser();
        long work = published(author, "뿌리");
        long clone = call("POST", "/works/" + work + "/clone", Map.of("mode", "FULL"), reader).json().get("projectId").asLong();

        long chapter = chapterId(clone, 1, reader);
        call("PUT", "/chapters/" + chapter, Map.of("title", "바꾼 제목", "content", "내가 바꾼 본문"), reader);
        JsonNode compare = call("GET", "/projects/" + clone + "/upstream", null, reader).json();
        assertThat(compare.get("available").asBoolean()).isTrue();
        assertThat(compare.get("chapters").get(0).get("state").asString()).isEqualTo("CHANGED");
        assertThat(compare.get("chapters").get(1).get("state").asString()).isEqualTo("SAME");

        // 원본이 업데이트되면 뒤처진 리비전이 보입니다.
        call("PUT", "/works/" + work + "/chapters/1", Map.of("title", "원본 수정", "content", "원본 본문"), author);
        assertThat(call("GET", "/projects/" + clone + "/upstream", null, reader).json().get("currentRevision").asInt()).isEqualTo(2);
        assertThat(call("GET", "/projects/" + clone + "/upstream", null, author).status()).isEqualTo(404);

        // 파생작을 게시하면 네트워크에 이어집니다.
        call("PUT", "/chapters/" + chapter + "/confirm", null, reader);
        long derived = call("POST", "/projects/" + clone + "/publish", publishBody("LONG", "파생작"), reader).json().get("id").asLong();
        assertThat(call("GET", "/works/" + derived, null, null).json().get("card").get("parentWorkId").asLong()).isEqualTo(work);

        JsonNode network = call("GET", "/works/" + derived + "/network", null, null).json();
        assertThat(network.get("rootId").asLong()).isEqualTo(work);
        assertThat(network.get("nodes")).hasSize(2);
        JsonNode rootView = call("GET", "/works/" + work + "/network", null, null).json();
        assertThat(rootView.get("full").asInt()).isEqualTo(1);
        assertThat(rootView.get("clones").get(0).get("derivedWorkId").asLong()).isEqualTo(derived);

        // 원본을 비공개로 돌리면 비교할 수 없습니다.
        Map<String, Object> meta = new HashMap<>(publishBody("LONG", "뿌리"));
        meta.put("visibility", "PRIVATE");
        call("PUT", "/works/" + work, meta, author);
        assertThat(call("GET", "/projects/" + clone + "/upstream", null, reader).json().get("available").asBoolean()).isFalse();
    }

    // ───────────── 공유 ─────────────

    @Test
    void shareGraphShowsWhoAndWhereAndCountsVisits() throws Exception {
        String author = newUser();
        String fan = newUser();
        long work = published(author, "공유");

        JsonNode created = call("POST", "/works/" + work + "/shares", Map.of("channel", "BLOG", "target", "내 블로그"), fan).json();
        String token = created.get("token").asString();
        call("POST", "/works/" + work + "/shares", Map.of("channel", "X"), author);
        assertThat(call("POST", "/shares/" + token + "/visit", null, null).status()).isEqualTo(204);
        call("POST", "/shares/" + token + "/visit", null, null);
        assertThat(call("POST", "/shares/없는토큰/visit".replace("없는토큰", "nope"), null, null).status()).isEqualTo(404);

        JsonNode graph = call("GET", "/works/" + work + "/shares", null, null).json();
        assertThat(graph.get("total").asInt()).isEqualTo(2);
        assertThat(graph.get("visits").asInt()).isEqualTo(2);
        JsonNode blog = graph.get("items").get(1);
        assertThat(blog.get("channel").asString()).isEqualTo("BLOG");
        assertThat(blog.get("target").asString()).isEqualTo("내 블로그");
        assertThat(blog.get("user").get("displayName").asString()).isEqualTo("테스터");
        assertThat(blog.has("token")).isFalse();
        assertThat(call("GET", "/works/" + work, null, null).json().get("card").get("shareCount").asInt()).isEqualTo(2);

        Map<String, Object> meta = new HashMap<>(publishBody("LONG", "공유"));
        meta.put("visibility", "PRIVATE");
        call("PUT", "/works/" + work, meta, author);
        assertThat(call("POST", "/works/" + work + "/shares", Map.of("channel", "LINK"), author).status()).isEqualTo(409);
    }

    // ───────────── 내보내기 ─────────────

    private record Download(int status, String contentType, byte[] bytes) {
    }

    private Download download(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<byte[]> r = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new Download(r.statusCode(), r.headers().firstValue("Content-Type").orElse(""), r.body());
    }

    private static Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> files = new java.util.LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                files.put(e.getName(), zip.readAllBytes());
            }
        }
        return files;
    }

    private static void wellFormed(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    @Test
    void exportSupportsDocumentFormatsAndEscapesMarkup() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        generateChapter(project, 1, token);
        long chapter = chapterId(project, 1, token);
        String body = "첫 문단 <script>alert(1)</script> & **굵게** \"인용\"\n\n> 인용 문단\n\n- 목록";
        call("PUT", "/chapters/" + chapter, Map.of("title", "장 <제목>", "content", body), token);
        call("PUT", "/chapters/" + chapter + "/confirm", null, token);
        long work = call("POST", "/projects/" + project + "/publish", publishBody("SHORT", "문서 & 형식"), token).json().get("id").asLong();

        for (String format : List.of("md", "txt", "docx", "pdf", "html", "epub", "odt", "rtf")) {
            assertThat(download("/works/" + work + "/export?format=" + format, token).status()).as(format).isEqualTo(200);
            assertThat(download("/projects/" + project + "/export?format=" + format, token).status()).as(format).isEqualTo(200);
        }
        assertThat(download("/works/" + work + "/export?format=exe", token).status()).isEqualTo(409);
        assertThat(download("/works/" + work + "/export?format=md", null).status()).isEqualTo(401);

        String html = new String(download("/works/" + work + "/export?format=html", token).bytes(), StandardCharsets.UTF_8);
        assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>").contains("<strong>굵게</strong>");

        assertThat(new String(download("/works/" + work + "/export?format=txt", token).bytes(), StandardCharsets.UTF_8))
                .contains("굵게").doesNotContain("**");

        byte[] rtf = download("/works/" + work + "/export?format=rtf", token).bytes();
        assertThat(new String(rtf, StandardCharsets.US_ASCII)).startsWith("{\\rtf1").contains("\\u").endsWith("}");

        Map<String, byte[]> epub = unzip(download("/works/" + work + "/export?format=epub", token).bytes());
        assertThat(epub.keySet().iterator().next()).isEqualTo("mimetype");
        assertThat(new String(epub.get("mimetype"), StandardCharsets.US_ASCII)).isEqualTo("application/epub+zip");
        assertThat(epub).containsKeys("META-INF/container.xml", "OEBPS/content.opf", "OEBPS/nav.xhtml", "OEBPS/ch1.xhtml");
        for (Map.Entry<String, byte[]> f : epub.entrySet()) {
            if (!f.getKey().equals("mimetype")) {
                wellFormed(f.getValue());
            }
        }

        Map<String, byte[]> odt = unzip(download("/works/" + work + "/export?format=odt", token).bytes());
        assertThat(odt.keySet().iterator().next()).isEqualTo("mimetype");
        wellFormed(odt.get("content.xml"));
        wellFormed(odt.get("META-INF/manifest.xml"));
        assertThat(new String(odt.get("content.xml"), StandardCharsets.UTF_8)).contains("&lt;script&gt;");

        // 작가가 막으면 다른 사용자는 받을 수 없습니다.
        Map<String, Object> meta = new HashMap<>(publishBody("SHORT", "문서 & 형식"));
        meta.put("downloadable", false);
        call("PUT", "/works/" + work, meta, token);
        assertThat(download("/works/" + work + "/export?format=md", newUser()).status()).isEqualTo(409);
        assertThat(download("/works/" + work + "/export?format=md", token).status()).isEqualTo(200);
    }

    // ───────────── 패키지포지 ─────────────

    private Map<String, Object> packageBody(String title, List<Long> members) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("packageType", "UNIVERSE");
        body.put("visibility", "PUBLIC");
        body.put("worldNote", "공용 세계관");
        body.put("memberWorkIds", members);
        return body;
    }

    @Test
    void packageForgeRequiresOwnWorkAndApprovalForOthers() throws Exception {
        String author = newUser();
        String other = newUser();
        long mine = published(author, "내 작품");
        long second = published(author, "내 둘째");
        long theirs = published(other, "남의 작품");

        assertThat(call("POST", "/packages", packageBody("남의 것만", List.of(theirs)), author).status()).isEqualTo(409);
        JsonNode pkg = call("POST", "/packages", packageBody("유니버스", List.of(mine, second)), author).json();
        long id = pkg.get("id").asLong();
        assertThat(pkg.get("kind").asString()).isEqualTo("PACKAGE");
        assertThat(pkg.get("chapterCount").asInt()).isEqualTo(2);

        // 다른 작가의 작품은 승인 전까지 독자에게 보이지 않습니다.
        long pending = call("POST", "/packages/" + id + "/items",
                Map.of("memberWorkId", theirs, "role", "SPINOFF", "note", "외전"), author).json().get("id").asLong();
        assertThat(call("GET", "/works/" + id, null, null).json().get("packageView").get("items")).hasSize(2);
        assertThat(call("GET", "/works/" + id, null, author).json().get("packageView").get("items")).hasSize(3);

        JsonNode requests = call("GET", "/me/package-requests", null, other).json();
        assertThat(requests.get(0).get("packageTitle").asString()).isEqualTo("유니버스");
        assertThat(call("PUT", "/me/package-requests/" + pending, Map.of("approve", true), author).status()).isEqualTo(404);
        assertThat(call("PUT", "/me/package-requests/" + pending, Map.of("approve", true), other).status()).isEqualTo(204);

        JsonNode view = call("GET", "/works/" + id, null, null).json().get("packageView");
        assertThat(view.get("items")).hasSize(3);
        assertThat(call("GET", "/works/" + id, null, null).json().get("card").get("chapterCount").asInt()).isEqualTo(3);
        assertThat(view.get("worldNote").asString()).isEqualTo("공용 세계관");
        // 같은 이름의 인물은 교차 등장으로 묶습니다 (스텁 Bible은 모두 한서윤).
        assertThat(view.get("characters").get(0).get("crossover").asBoolean()).isTrue();
        assertThat(view.get("characters").get(0).get("appearances")).hasSize(3);
        assertThat(call("GET", "/works/" + theirs, null, null).json().get("inPackages").get(0).get("id").asLong()).isEqualTo(id);

        long first = view.get("items").get(0).get("id").asLong();
        long last = view.get("items").get(2).get("id").asLong();
        long middle = view.get("items").get(1).get("id").asLong();
        Map<String, Object> link = Map.of("fromItemId", first, "toItemId", last, "type", "SAME_EVENT", "label", "3장 사건");
        assertThat(call("POST", "/packages/" + id + "/links", link, other).status()).isEqualTo(404);
        assertThat(call("POST", "/packages/" + id + "/links", link, author).status()).isEqualTo(201);
        assertThat(call("POST", "/packages/" + id + "/links", Map.of("fromItemId", first, "toItemId", first, "type", "CONTINUES"), author).status()).isEqualTo(409);

        assertThat(call("PUT", "/packages/" + id + "/order", Map.of("itemIds", List.of(last, first)), author).status()).isEqualTo(409);
        assertThat(call("PUT", "/packages/" + id + "/order", Map.of("itemIds", List.of(last, first, middle)), author).status()).isEqualTo(204);
        assertThat(call("GET", "/works/" + id, null, null).json().get("packageView").get("items").get(0).get("id").asLong()).isEqualTo(last);

        // 패키지는 패키지를 담지 못하고 Clone도 되지 않습니다.
        long nested = call("POST", "/packages", packageBody("다른 묶음", List.of(mine)), author).json().get("id").asLong();
        assertThat(call("POST", "/packages/" + id + "/items", Map.of("memberWorkId", nested, "role", "MAIN"), author).status()).isEqualTo(409);
        assertThat(call("POST", "/works/" + id + "/clone", Map.of("mode", "WORLD"), other).status()).isEqualTo(409);

        // 통합 내보내기는 독서 순서대로 묶습니다.
        String merged = new String(download("/works/" + id + "/export?format=md", other).bytes(), StandardCharsets.UTF_8);
        assertThat(merged.indexOf("남의 작품 — 1장")).isLessThan(merged.indexOf("내 작품 — 1장")).isNotNegative();

        // 편입된 작가는 자기 작품을 뺄 수 있습니다.
        assertThat(call("DELETE", "/packages/" + id + "/items/" + last, null, other).status()).isEqualTo(204);
        assertThat(call("GET", "/works/" + id, null, null).json().get("packageView").get("items")).hasSize(2);
    }

    // ───────────── 추천, 이슈, 댓글, 게시글 ─────────────

    @Test
    void likesAreIdempotentAndDailyMilestoneReachesIssueBoard() throws Exception {
        String author = newUser();
        String fan = newUser();
        long work = published(author, "인기작");

        assertThat(call("PUT", "/works/" + work + "/like", null, author).status()).isEqualTo(409);
        assertThat(call("PUT", "/works/" + work + "/like", null, fan).json().get("likeCount").asInt()).isEqualTo(1);
        assertThat(call("PUT", "/works/" + work + "/like", null, fan).json().get("likeCount").asInt()).isEqualTo(1);
        assertThat(call("GET", "/works/" + work, null, fan).json().get("liked").asBoolean()).isTrue();
        assertThat(call("DELETE", "/works/" + work + "/like", null, fan).json().get("likeCount").asInt()).isZero();
        assertThat(call("DELETE", "/works/" + work + "/like", null, fan).json().get("likeCount").asInt()).isZero();

        // 하루 추천 99개를 넣어 두고 100번째를 API로 누릅니다.
        for (int i = 0; i < 99; i++) {
            Long user = jdbc.queryForObject("INSERT INTO app_user (email, password_hash, display_name, created_at) VALUES (?, 'x', '팬', now()) RETURNING id",
                    Long.class, "fan" + UUID.randomUUID() + "@example.com");
            jdbc.update("INSERT INTO work_like (work_id, user_id, created_at) VALUES (?, ?, now())", work, user);
        }
        jdbc.update("UPDATE work SET like_count = 99 WHERE id = ?", work);
        call("PUT", "/works/" + work + "/like", null, fan);

        JsonNode board = call("GET", "/issues", null, null).json();
        List<String> messages = new ArrayList<>();
        board.get("celebrations").forEach(c -> {
            if (c.get("work").get("id").asLong() == work) {
                messages.add(c.get("message").asString());
            }
        });
        assertThat(messages).containsExactlyInAnyOrder("하루 만에 추천 100개를 받았습니다", "누적 추천 100개를 달성했습니다");
        assertThat(board.get("trending").get(0).get("id").asLong()).isEqualTo(work);

        // 같은 단위는 다시 기록하지 않습니다.
        call("DELETE", "/works/" + work + "/like", null, fan);
        call("PUT", "/works/" + work + "/like", null, fan);
        Integer count = jdbc.queryForObject("SELECT count(*) FROM issue_event WHERE work_id = ?", Integer.class, work);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void commentsAndPostsEnforceOwnership() throws Exception {
        String author = newUser();
        String reader = newUser();
        String stranger = newUser();
        long work = published(author, "댓글");

        long comment = call("POST", "/works/" + work + "/comments", Map.of("content", "<b>재밌어요</b>"), reader).json().get("id").asLong();
        long reply = call("POST", "/works/" + work + "/comments", Map.of("content", "답글", "parentId", comment), author).json().get("id").asLong();
        assertThat(call("POST", "/works/" + work + "/comments", Map.of("content", "x", "parentId", reply), reader).status()).isEqualTo(409);
        assertThat(call("GET", "/works/" + work, null, null).json().get("card").get("commentCount").asInt()).isEqualTo(2);
        assertThat(call("GET", "/works/" + work + "/comments", null, null).json().get(0).get("content").asString()).isEqualTo("<b>재밌어요</b>");

        assertThat(call("PUT", "/comments/" + comment, Map.of("content", "수정"), stranger).status()).isEqualTo(404);
        assertThat(call("PUT", "/comments/" + comment, Map.of("content", "수정"), author).status()).isEqualTo(404);
        assertThat(call("PUT", "/comments/" + comment, Map.of("content", "수정"), reader).status()).isEqualTo(200);
        assertThat(call("DELETE", "/comments/" + comment, null, stranger).status()).isEqualTo(404);
        // 작품 주인은 지울 수 있고 답글도 함께 지워집니다.
        assertThat(call("DELETE", "/comments/" + comment, null, author).status()).isEqualTo(204);
        assertThat(call("GET", "/works/" + work, null, null).json().get("card").get("commentCount").asInt()).isZero();

        long post = call("POST", "/posts", Map.of("category", "RECRUIT", "title", "모집", "content", "함께 쓸 분"), reader).json().get("id").asLong();
        assertThat(call("GET", "/posts?category=RECRUIT", null, null).json().get("total").asInt()).isPositive();
        assertThat(call("GET", "/posts/" + post, null, null).json().get("content").asString()).isEqualTo("함께 쓸 분");
        Map<String, Object> edit = Map.of("category", "FREE", "title", "수정", "content", "내용");
        assertThat(call("PUT", "/posts/" + post, edit, stranger).status()).isEqualTo(404);
        assertThat(call("PUT", "/posts/" + post, edit, reader).status()).isEqualTo(200);
        assertThat(call("PUT", "/posts/" + post + "/like", null, reader).status()).isEqualTo(409);
        assertThat(call("PUT", "/posts/" + post + "/like", null, stranger).json().get("likeCount").asInt()).isEqualTo(1);
        call("POST", "/posts/" + post + "/comments", Map.of("content", "좋아요"), stranger);
        assertThat(call("GET", "/posts/" + post, null, stranger).json().get("card").get("commentCount").asInt()).isEqualTo(1);
        assertThat(call("DELETE", "/posts/" + post, null, stranger).status()).isEqualTo(404);
        assertThat(call("DELETE", "/posts/" + post, null, reader).status()).isEqualTo(204);
    }

    // ───────────── 프로필, 계정 ─────────────

    @Test
    void profileHidesPrivateActivityAndEarnsAchievements() throws Exception {
        String token = newUser();
        long user = userId(call("GET", "/auth/me", null, token));
        long project = call("POST", "/projects", Map.of("title", "비밀 프로젝트"), token).json().get("id").asLong();
        long work = published(token, "공개 작품");

        Response profile = call("GET", "/users/" + user, null, null);
        assertThat(profile.status()).isEqualTo(200);
        assertThat(profile.body()).doesNotContain("@example.com");
        assertThat(profile.json().get("stats").get("works").asInt()).isEqualTo(1);
        assertThat(call("GET", "/users/" + user + "/works", null, null).json().get(0).get("id").asLong()).isEqualTo(work);
        assertThat(call("GET", "/users/999999", null, null).status()).isEqualTo(404);

        JsonNode grass = call("GET", "/users/" + user + "/contributions", null, null).json();
        assertThat(grass.get("total").asInt()).isGreaterThan(5);
        assertThat(grass.get("currentStreak").asInt()).isEqualTo(1);
        assertThat(grass.get("days").get(0).get("level").asInt()).isBetween(1, 4);
        String today = grass.get("to").asString();

        // 다른 사람은 비공개 프로젝트 기록을 볼 수 없습니다.
        String publicFeed = call("GET", "/users/" + user + "/activity?size=50", null, null).body();
        assertThat(publicFeed).contains("공개 작품").doesNotContain("비밀 프로젝트");
        JsonNode ownFeed = call("GET", "/users/" + user + "/activity?size=50&date=" + today, null, token).json();
        List<Long> projectIds = new ArrayList<>();
        ownFeed.get("items").forEach(e -> {
            if (!e.get("projectId").isNull()) {
                projectIds.add(e.get("projectId").asLong());
            }
        });
        assertThat(projectIds).contains(project);
        assertThat(call("GET", "/users/" + user + "/activity?date=어제", null, token).status()).isEqualTo(409);

        Map<String, String> earned = new HashMap<>();
        call("GET", "/users/" + user + "/achievements", null, null).json().forEach(a -> {
            if (!a.get("earnedAt").isNull()) {
                earned.put(a.get("code").asString(), a.get("name").asString());
            }
        });
        assertThat(earned).containsKeys("FIRST_PROJECT", "FIRST_AGENT_CHAPTER", "COMPLETED_1", "FIRST_PUBLISH", "FIRST_LONG");
        assertThat(earned).doesNotContainKey("LIKES_10");
    }

    @Test
    void accountSettingsValidateAndDeleteCascades() throws Exception {
        String email = "acc" + UUID.randomUUID() + "@example.com";
        String token = call("POST", "/auth/register", Map.of("email", email, "acceptTerms", true, "password", "password123", "displayName", "이전"), null)
                .json().get("accessToken").asString();
        long user = userId(call("GET", "/auth/me", null, token));

        Map<String, Object> bad = new HashMap<>();
        bad.put("displayName", "이름");
        bad.put("website", "javascript:alert(1)");
        assertThat(call("PUT", "/me/profile", bad, token).status()).isEqualTo(400);
        Map<String, Object> good = new HashMap<>();
        good.put("displayName", "새 이름");
        good.put("bio", "소개");
        good.put("website", "https://example.com");
        good.put("avatarColor", "#2e75b6");
        assertThat(call("PUT", "/me/profile", good, token).json().get("displayName").asString()).isEqualTo("새 이름");
        assertThat(call("GET", "/users/" + user, null, null).json().get("bio").asString()).isEqualTo("소개");

        assertThat(call("PUT", "/me/password", Map.of("currentPassword", "wrong-pass", "newPassword", "newpassword1"), token).status()).isEqualTo(409);
        assertThat(call("PUT", "/me/password", Map.of("currentPassword", "password123", "newPassword", "newpassword1"), token).status()).isEqualTo(204);
        assertThat(call("POST", "/auth/login", Map.of("email", email, "password", "password123"), null).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/login", Map.of("email", email, "password", "newpassword1"), null).status()).isEqualTo(200);

        long work = published(token, "지워질 작품");
        String other = newUser();
        long clone = call("POST", "/works/" + work + "/clone", Map.of("mode", "WORLD"), other).json().get("projectId").asLong();

        assertThat(call("POST", "/me/delete", Map.of("password", "wrong-pass"), token).status()).isEqualTo(409);
        assertThat(call("POST", "/me/delete", Map.of("password", "newpassword1"), token).status()).isEqualTo(204);
        assertThat(call("GET", "/users/" + user, null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/works/" + work, null, null).status()).isEqualTo(404);
        // 다른 사람의 Clone은 출처 이름과 함께 남습니다.
        JsonNode kept = call("GET", "/projects/" + clone, null, other).json();
        assertThat(kept.get("clonedFromTitle").asString()).isEqualTo("지워질 작품");
        assertThat(kept.get("clonedFromWorkId").isNull()).isTrue();
        assertThat(call("GET", "/projects/" + clone + "/upstream", null, other).json().get("available").asBoolean()).isFalse();
    }

    @Test
    void projectListShowsWorkshopFields() throws Exception {
        String author = newUser();
        long short1 = call("POST", "/projects", Map.of("title", "단편", "form", "SHORT"), author).json().get("id").asLong();
        long project = projectWithChapters(author, 1, 1);
        long work = call("POST", "/projects/" + project + "/publish", publishBody("SHORT", "게시"), author).json().get("id").asLong();

        Map<Long, JsonNode> byId = new HashMap<>();
        call("GET", "/projects", null, author).json().forEach(p -> byId.put(p.get("id").asLong(), p));
        assertThat(byId.get(short1).get("form").asString()).isEqualTo("SHORT");
        assertThat(byId.get(project).get("workId").asLong()).isEqualTo(work);
        assertThat(byId.get(project).get("agentChars").asLong()).isPositive();
    }

    @Test
    void guideChatRequiresLoginAndDropsCollabLinkWithoutConfiguredUrl() throws Exception {
        assertThat(call("POST", "/guide/chat", Map.of("message", "/help"), null).status()).isEqualTo(401);
        JsonNode reply = call("POST", "/guide/chat", Map.of("message", "/collab"), newUser()).json();
        assertThat(reply.get("reply").asString()).contains("연결 주소가 아직 설정되지 않아");
        assertThat(reply.get("actions")).hasSize(1);
        assertThat(reply.get("actions").get(0).get("kind").asString()).isEqualTo("route");
    }

    @Test
    void collaboratorsShareTheDraftAndSeeTheCommitSpan() throws Exception {
        String email = "mate" + UUID.randomUUID() + "@example.com";
        String mate = call("POST", "/auth/register", Map.of(
                "email", email, "acceptTerms", true, "password", "password123", "displayName", "동료"), null).json().get("accessToken").asString();
        String owner = newUser();
        long project = call("POST", "/projects", Map.of("title", "함께", "genre", "판타지"), owner).json().get("id").asLong();
        assertThat(call("GET", "/projects/" + project, null, mate).status()).isEqualTo(404);

        Response invite = call("POST", "/projects/" + project + "/collaborators", Map.of("email", email), owner);
        assertThat(invite.status()).isEqualTo(201);
        long invitation = invite.json().get("id").asLong();
        assertThat(call("GET", "/projects/" + project, null, mate).status()).isEqualTo(404);

        assertThat(call("POST", "/me/invitations/" + invitation + "/accept", null, mate).status()).isEqualTo(200);
        assertThat(call("GET", "/projects/" + project, null, mate).status()).isEqualTo(200);
        assertThat(call("GET", "/projects", null, mate).json().get(0).get("role").asString()).isEqualTo("COLLABORATOR");
        assertThat(call("DELETE", "/projects/" + project, null, mate).status()).isEqualTo(409);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "첫 문단을 고쳤다");
        body.put("target", "CHAPTER");
        body.put("chapterNumber", 1);
        body.put("startOffset", 0);
        body.put("endOffset", 2);
        body.put("quote", "문단시작");
        Response commit = call("POST", "/projects/" + project + "/commits", body, mate);
        assertThat(commit.status()).isEqualTo(201);
        assertThat(commit.json().get("quote").asString()).isEqualTo("문단시작");
        assertThat(commit.json().get("endOffset").asInt()).isEqualTo(2);

        JsonNode listed = call("GET", "/projects/" + project + "/commits", null, owner).json();
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).get("message").asString()).isEqualTo("첫 문단을 고쳤다");
        assertThat(call("GET", "/projects/" + project + "/commits", null, newUser()).status()).isEqualTo(404);

        JsonNode graph = call("GET", "/projects/" + project + "/activity", null, owner).json();
        assertThat(graph.get("events").toString()).contains("작업 커밋");
    }
}
