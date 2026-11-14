package com.storyforge.client;

import com.storyforge.exception.AiServiceException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 브라우저 세션 하나당 FastAPI 웹소켓 하나를 유지합니다.
 * 챕터 본문 스트림은 기존 SSE를 유지합니다.
 */
@Component
public class AiLiveClient {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]*)\"");

    private final URI endpoint;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public AiLiveClient(AiServiceProperties properties) {
        String base = properties.baseUrl().replaceFirst("^http", "ws");
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.endpoint = URI.create(base + "/internal/ws");
    }

    public Session open() {
        Session session = new Session();
        try {
            http.newWebSocketBuilder().buildAsync(endpoint, session).get(5, TimeUnit.SECONDS);
            session.opened.get(5, TimeUnit.SECONDS);
            return session;
        } catch (Exception e) {
            throw new AiServiceException("AI 실시간 연결에 실패했습니다.", e);
        }
    }

    public final class Session implements WebSocket.Listener, AutoCloseable {
        private final CompletableFuture<WebSocket> opened = new CompletableFuture<>();
        private final ConcurrentHashMap<String, CompletableFuture<String>> waiters = new ConcurrentHashMap<>();
        private final StringBuilder buffer = new StringBuilder();
        private volatile WebSocket socket;

        public String exchange(String json) {
            WebSocket current = socket;
            if (current == null) {
                throw new AiServiceException("AI 실시간 연결에 실패했습니다.", null);
            }
            Matcher matcher = ID.matcher(json);
            if (!matcher.find()) {
                throw new AiServiceException("실시간 요청에 id가 없습니다.", null);
            }
            CompletableFuture<String> done = new CompletableFuture<>();
            waiters.put(matcher.group(1), done);
            current.sendText(json, true);
            try {
                return done.get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                waiters.remove(matcher.group(1), done);
                throw new AiServiceException("AI 실시간 연결에 실패했습니다.", e);
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            opened.complete(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = null;
            synchronized (buffer) {
                buffer.append(data);
                if (last) {
                    message = buffer.toString();
                    buffer.setLength(0);
                }
            }
            webSocket.request(1);
            if (message != null) {
                Matcher matcher = ID.matcher(message);
                CompletableFuture<String> done = matcher.find() ? waiters.remove(matcher.group(1)) : null;
                if (done != null) {
                    done.complete(message);
                }
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            opened.completeExceptionally(error);
            waiters.forEach((id, done) -> done.completeExceptionally(error));
            waiters.clear();
        }

        @Override
        public void close() {
            WebSocket current = socket;
            if (current != null) {
                current.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            }
            waiters.forEach((id, done) -> done.completeExceptionally(new IllegalStateException("연결이 닫혔습니다.")));
            waiters.clear();
        }
    }
}
