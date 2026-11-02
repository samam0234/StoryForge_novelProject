package com.storyforge.support;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * 실제 PostgreSQL + 실제 HTTP로 검증하는 통합 테스트 기반. AI 서비스는 StubAiServer가 대신합니다.
 * Docker가 없으면 건너뜁니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    protected static final StubAiServer AI = new StubAiServer();
    protected static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static {
        // 모든 테스트 클래스가 컨테이너 하나를 공유합니다 (JVM 종료 시 정리).
        POSTGRES.start();
        AI.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("storyforge.ai.base-url", AI::baseUrl);
        // 테스트는 같은 IP에서 많이 호출합니다. 제한 동작은 RateLimitFilterTest에서 검증합니다.
        registry.add("storyforge.rate-limit.auth-per-minute", () -> 100_000);
        registry.add("storyforge.rate-limit.generate-per-minute", () -> 100_000);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected CircuitBreaker aiCircuitBreaker;

    @BeforeEach
    void resetAi() {
        AI.reset();
        aiCircuitBreaker.reset();
    }

    protected Response call(String method, String path, Object body, String token) throws Exception {
        return call(method, path, body, token, "application/json");
    }

    protected Response call(String method, String path, Object body, String token, String accept) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Accept", accept);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    /** 새 사용자를 만들고 토큰을 돌려줍니다. */
    protected String newUser() throws Exception {
        Response r = call("POST", "/auth/register", Map.of(
                "email", "u" + UUID.randomUUID() + "@example.com",
                "password", "password123",
                "displayName", "테스터"), null);
        return r.json().get("accessToken").asString();
    }

    /** 프로젝트를 Outline 확정 단계까지 진행하고 ID를 돌려줍니다. */
    protected long projectAtOutlineConfirmed(String token, int chapters) throws Exception {
        long id = call("POST", "/projects", Map.of("title", "테스트", "genre", "판타지"), token).json().get("id").asLong();
        call("POST", "/projects/" + id + "/bible/generate", Map.of("prompt", "아이디어"), token);
        call("PUT", "/projects/" + id + "/bible/confirm", null, token);
        call("POST", "/projects/" + id + "/outline/generate", Map.of("chapterCount", chapters), token);
        call("PUT", "/projects/" + id + "/outline/confirm", null, token);
        return id;
    }

    protected Response generateChapter(long projectId, int number, String token) throws Exception {
        return call("POST", "/projects/" + projectId + "/chapters/" + number + "/generate", null, token,
                "text/event-stream, application/problem+json");
    }

    protected String status(long projectId, String token) throws Exception {
        return call("GET", "/projects/" + projectId + "/status", null, token).json().get("status").asString();
    }

    protected long chapterId(long projectId, int number, String token) throws Exception {
        for (JsonNode c : call("GET", "/projects/" + projectId + "/chapters", null, token).json()) {
            if (c.get("number").asInt() == number) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("챕터 없음: " + number);
    }

    public record Response(int status, String body) {
        public JsonNode json() {
            return JSON.readTree(body);
        }
    }
}
