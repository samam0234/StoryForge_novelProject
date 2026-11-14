package com.storyforge.controller;

import com.storyforge.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void mailIsExplicitlyUnavailableWithoutConfiguration() throws Exception {
        assertThat(call("GET", "/auth/mail/status", null, null).json().get("enabled").asBoolean()).isFalse();
        assertThat(call("POST", "/auth/mail/reset-request", Map.of("email", "test@example.test"), null).status()).isEqualTo(503);
    }

    private static String email() {
        return "a" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registerReturnsTokenThatAuthorizesRequests() throws Exception {
        String email = email();
        Response r = call("POST", "/auth/register", Map.of("email", email, "acceptTerms", true, "password", "password123", "displayName", "가"), null);
        assertThat(r.status()).isEqualTo(201);
        String token = r.json().get("accessToken").asString();
        assertThat(r.body()).doesNotContain("password");

        Response me = call("GET", "/auth/me", null, token);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.json().get("email").asString()).isEqualTo(email);
    }

    @Test
    void emailIsCaseInsensitiveForDuplicateCheckAndLogin() throws Exception {
        String email = email();
        call("POST", "/auth/register", Map.of("email", email, "acceptTerms", true, "password", "password123", "displayName", "가"), null);

        assertThat(call("POST", "/auth/register",
                Map.of("email", email.toUpperCase(), "acceptTerms", true, "password", "password123", "displayName", "나"), null).status()).isEqualTo(409);
        assertThat(call("POST", "/auth/login",
                Map.of("email", email.toUpperCase(), "password", "password123"), null).status()).isEqualTo(200);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameResponse() throws Exception {
        String email = email();
        call("POST", "/auth/register", Map.of("email", email, "acceptTerms", true, "password", "password123", "displayName", "가"), null);

        Response wrong = call("POST", "/auth/login", Map.of("email", email, "password", "wrong-password"), null);
        Response unknown = call("POST", "/auth/login", Map.of("email", email(), "password", "wrong-password"), null);
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(unknown.status()).isEqualTo(401);
        assertThat(wrong.json().get("detail").asString()).isEqualTo(unknown.json().get("detail").asString());
    }

    @Test
    void rejectsInvalidRegistration() throws Exception {
        assertThat(call("POST", "/auth/register",
                Map.of("email", email(), "acceptTerms", true, "password", "short", "displayName", "가"), null).status()).isEqualTo(400);
        assertThat(call("POST", "/auth/register",
                Map.of("email", "not-an-email", "acceptTerms", true, "password", "password123", "displayName", "가"), null).status()).isEqualTo(400);
    }

    @Test
    void refreshRotatesAndPasswordChangeRevokes() throws Exception {
        String email = email();
        Response registered = call("POST", "/auth/register",
                Map.of("email", email, "acceptTerms", true, "password", "password123", "displayName", "가"), null);
        String refresh = registered.json().get("refreshToken").asString();
        assertThat(refresh).isNotBlank();

        Response rotated = call("POST", "/auth/refresh", Map.of("refreshToken", refresh), null);
        assertThat(rotated.status()).isEqualTo(200);
        String next = rotated.json().get("refreshToken").asString();
        assertThat(next).isNotEqualTo(refresh);
        assertThat(call("GET", "/auth/me", null, rotated.json().get("accessToken").asString()).status()).isEqualTo(200);
        assertThat(call("POST", "/auth/refresh", Map.of("refreshToken", refresh), null).status()).isEqualTo(401);

        assertThat(call("PUT", "/me/password", Map.of("currentPassword", "password123", "newPassword", "password456"),
                rotated.json().get("accessToken").asString()).status()).isEqualTo(204);
        assertThat(call("POST", "/auth/refresh", Map.of("refreshToken", next), null).status()).isEqualTo(401);
        assertThat(call("POST", "/auth/logout", Map.of("refreshToken", next), null).status()).isEqualTo(204);
    }

    @Test
    void prometheusStaysHiddenWithoutMetricsToken() throws Exception {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("http://localhost:" + port + "/actuator/prometheus")).GET().build();
        java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
                .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void apiRequiresValidToken() throws Exception {
        assertThat(call("GET", "/projects", null, null).status()).isEqualTo(401);
        Response bad = call("GET", "/projects", null, "not-a-token");
        assertThat(bad.status()).isEqualTo(401);
        assertThat(bad.json().get("detail").asString()).isNotBlank();
    }

    @Test
    void otherUsersResourcesLookMissing() throws Exception {
        String owner = newUser();
        String other = newUser();
        long project = projectAtOutlineConfirmed(owner, 1);
        generateChapter(project, 1, owner);
        long chapter = chapterId(project, 1, owner);

        for (String path : new String[]{
                "/projects/" + project, "/projects/" + project + "/status", "/projects/" + project + "/bible",
                "/projects/" + project + "/outline", "/projects/" + project + "/chapters", "/chapters/" + chapter}) {
            assertThat(call("GET", path, null, owner).status()).as(path).isEqualTo(200);
            assertThat(call("GET", path, null, other).status()).as(path).isEqualTo(404);
        }
        assertThat(call("PUT", "/chapters/" + chapter + "/confirm", null, other).status()).isEqualTo(404);
        assertThat(call("DELETE", "/projects/" + project, null, other).status()).isEqualTo(404);
        assertThat(generateChapter(project, 1, other).status()).isEqualTo(404);
        assertThat(call("GET", "/projects", null, other).json().size()).isZero();
        assertThat(call("GET", "/projects", null, owner).json().size()).isEqualTo(1);
    }
}
