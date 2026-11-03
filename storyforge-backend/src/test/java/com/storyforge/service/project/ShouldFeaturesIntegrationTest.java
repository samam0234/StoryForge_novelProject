package com.storyforge.service.project;

import com.storyforge.support.IntegrationTestBase;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFeaturesIntegrationTest extends IntegrationTestBase {

    private HttpResponse<byte[]> download(long project, String format, String token) throws Exception {
        String query = format == null ? "" : "?format=" + format;
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/projects/" + project + "/export" + query))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private long projectWithConfirmedChapter(String token, String markdown) throws Exception {
        long project = projectAtOutlineConfirmed(token, 2);
        generateChapter(project, 1, token);
        long chapter = chapterId(project, 1, token);
        call("PUT", "/chapters/" + chapter, Map.of("title", "시작", "content", markdown), token);
        call("PUT", "/chapters/" + chapter + "/confirm", null, token);
        return project;
    }

    @Test
    void exportRequiresConfirmedChapterAndOmitsDrafts() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 2);
        assertThat(download(project, "md", token).statusCode()).isEqualTo(409);

        generateChapter(project, 1, token);
        assertThat(download(project, "md", token).statusCode()).as("초안만 있음").isEqualTo(409);

        call("PUT", "/chapters/" + chapterId(project, 1, token) + "/confirm", null, token);
        generateChapter(project, 2, token);
        String md = new String(download(project, "md", token).body(), StandardCharsets.UTF_8);
        assertThat(md).startsWith("# 테스트").contains("## 1장.").doesNotContain("## 2장.");
    }

    @Test
    void exportsMarkdownWithKoreanFilename() throws Exception {
        String token = newUser();
        long project = projectWithConfirmedChapter(token, "첫 문단\n\n**굵게**");
        HttpResponse<byte[]> res = download(project, "md", token);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/markdown");
        assertThat(res.headers().firstValue("Content-Disposition").orElseThrow()).contains("attachment").contains("UTF-8");
        assertThat(new String(res.body(), StandardCharsets.UTF_8)).contains("## 1장. 시작").contains("**굵게**");
    }

    @Test
    void exportsDocxWithStructure() throws Exception {
        String token = newUser();
        long project = projectWithConfirmedChapter(token, "일반 문단\n\n**굵은** 문장\n\n- 목록 항목\n\n> 인용");
        HttpResponse<byte[]> res = download(project, "docx", token);
        assertThat(res.statusCode()).isEqualTo(200);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(res.body()))) {
            String text = doc.getParagraphs().stream().map(p -> p.getText()).collect(Collectors.joining("\n"));
            assertThat(text).contains("테스트").contains("1장. 시작").contains("일반 문단").contains("• 목록 항목").contains("인용");
            assertThat(text).doesNotContain("**");
            boolean hasBoldRun = doc.getParagraphs().stream().flatMap(p -> p.getRuns().stream())
                    .anyMatch(r -> "굵은".equals(r.text()) && r.isBold());
            assertThat(hasBoldRun).isTrue();
        }
    }

    @Test
    void exportsPdfWithExtractableKorean() throws Exception {
        String token = newUser();
        long project = projectWithConfirmedChapter(token, "한글 문단입니다.");
        HttpResponse<byte[]> res = download(project, "pdf", token);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(new String(res.body(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (PdfReader reader = new PdfReader(res.body())) {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                text.append(extractor.getTextFromPage(i));
            }
            assertThat(text.toString()).contains("한글 문단입니다");
        }
    }

    @Test
    void exportUsesProjectDefaultAndRejectsUnknownFormat() throws Exception {
        String token = newUser();
        long project = projectWithConfirmedChapter(token, "내용");
        assertThat(download(project, "exe", token).statusCode()).isEqualTo(409);
        call("PUT", "/projects/" + project, Map.of("title", "테스트", "exportFormat", "pdf"), token);
        assertThat(download(project, null, token).headers().firstValue("Content-Type").orElseThrow()).isEqualTo("application/pdf");
    }

    @Test
    void exportOfOtherUsersProjectIsNotFound() throws Exception {
        String owner = newUser();
        long project = projectWithConfirmedChapter(owner, "내용");
        assertThat(download(project, "md", newUser()).statusCode()).isEqualTo(404);
    }

    @Test
    void chapterHistoryRecordsGenerationsAndEdits() throws Exception {
        String token = newUser();
        long project = projectAtOutlineConfirmed(token, 1);
        generateChapter(project, 1, token);
        long chapter = chapterId(project, 1, token);
        call("PUT", "/chapters/" + chapter, Map.of("title", "제목", "content", "수정본"), token);
        call("PUT", "/chapters/" + chapter, Map.of("title", "제목", "content", "수정본"), token);
        generateChapter(project, 1, token);

        JsonNode versions = call("GET", "/chapters/" + chapter + "/versions", null, token).json();
        assertThat(versions.size()).as("내용이 같은 저장은 이력을 남기지 않음").isEqualTo(3);
        assertThat(versions.get(0).get("source").asString()).isEqualTo("GENERATED");
        assertThat(versions.get(0).get("version").asInt()).isEqualTo(3);
        assertThat(versions.get(1).get("source").asString()).isEqualTo("EDITED");
        assertThat(versions.get(1).get("content").asString()).isEqualTo("수정본");
        assertThat(call("GET", "/chapters/" + chapter + "/versions", null, newUser()).status()).isEqualTo(404);
    }

    @Test
    void bibleHistoryKeepsEveryVersion() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();
        call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token);
        call("PUT", "/projects/" + project + "/bible", Map.of("worldSetting", "새 세계", "theme", "t", "tone", "o",
                "characters", java.util.List.of(Map.of("name", "새 인물"))), token);

        JsonNode versions = call("GET", "/projects/" + project + "/bible/versions", null, token).json();
        assertThat(versions.size()).isEqualTo(2);
        assertThat(versions.get(0).get("version").asInt()).isEqualTo(2);
        assertThat(versions.get(0).get("snapshot").get("characters").get(0).get("name").asString()).isEqualTo("새 인물");
        assertThat(versions.get(1).get("snapshot").get("worldSetting").asString()).isEqualTo("세계");
    }

    @Test
    void settingsAreValidatedAndReachAiService() throws Exception {
        String token = newUser();
        long project = call("POST", "/projects", Map.of("title", "t"), token).json().get("id").asLong();

        assertThat(call("PUT", "/projects/" + project, Map.of("title", "t", "model", "gpt-4", "exportFormat", "md"), token).status()).isEqualTo(400);
        assertThat(call("PUT", "/projects/" + project, Map.of("title", "t", "exportFormat", "exe"), token).status()).isEqualTo(400);

        Response ok = call("PUT", "/projects/" + project, Map.of("title", "새 제목", "genre", "SF", "model", "claude-sonnet-5-5",
                "defaultTone", "건조한 문체", "exportFormat", "docx"), token);
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.json().get("title").asString()).isEqualTo("새 제목");
        assertThat(ok.json().get("exportFormat").asString()).isEqualTo("docx");

        call("POST", "/projects/" + project + "/bible/generate", Map.of("prompt", "p"), token);
        call("PUT", "/projects/" + project + "/bible/confirm", null, token);
        call("POST", "/projects/" + project + "/outline/generate", Map.of("chapterCount", 1), token);
        call("PUT", "/projects/" + project + "/outline/confirm", null, token);
        generateChapter(project, 1, token);
        JsonNode sent = JSON.readTree(AI.chapterRequests.get(0)).get("project");
        assertThat(sent.get("model").asString()).isEqualTo("claude-sonnet-5-5");
        assertThat(sent.get("defaultTone").asString()).isEqualTo("건조한 문체");
    }
}
