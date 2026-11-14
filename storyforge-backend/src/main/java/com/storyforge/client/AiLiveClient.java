package com.storyforge.client;

import com.storyforge.exception.AiServiceException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Spring Boot에서 FastAPI 내부 웹소켓으로 요청 하나를 보내고 응답 하나를 받습니다.
 * 챕터 스트림은 기존 SSE를 유지합니다.
 */
@Component
public class AiLiveClient {

    private final URI endpoint;

    public AiLiveClient(AiServiceProperties properties) {
        String base = properties.baseUrl().replaceFirst("^http", "ws");
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.endpoint = URI.create(base + "/internal/ws");
    }

    public String exchange(String json) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        CompletableFuture<String> done = new CompletableFuture<>();
        StringBuilder body = new StringBuilder();
        client.newWebSocketBuilder().buildAsync(endpoint, new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.sendText(json, true);
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                body.append(data);
                if (last) {
                    done.complete(body.toString());
                } else {
                    webSocket.request(1);
                }
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                done.completeExceptionally(error);
            }
        }).whenComplete((socket, error) -> {
            if (error != null) {
                done.completeExceptionally(error);
            }
        });
        try {
            return done.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AiServiceException("AI 실시간 연결에 실패했습니다.", e);
        }
    }
}
