package com.storyforge.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FastAPI 내부 API를 흉내 내는 테스트 서버. 실제 HTTP/SSE로 응답합니다.
 */
public class StubAiServer {

    public enum ChapterMode { OK, SSE_ERROR, HTTP_500 }

    private HttpServer server;
    private int boundPort = 0;
    private volatile ChapterMode chapterMode = ChapterMode.OK;
    private volatile boolean failSync = false;
    private volatile long chapterDelayMillis = 0;
    private volatile boolean reviewFindsHighIssue = false;
    private volatile boolean failReview = false;
    /** 받은 재검증 요청 본문 */
    public final List<String> reviewRequests = new CopyOnWriteArrayList<>();
    private final AtomicInteger syncCalls = new AtomicInteger();
    /** 받은 챕터 요청 본문 (컨텍스트 주입 검증용) */
    public final List<String> chapterRequests = new CopyOnWriteArrayList<>();

    /** 연결 실패 재현: 같은 포트로 다시 열 수 있게 멈춥니다. */
    public void stop() {
        boundPort = server.getAddress().getPort();
        server.stop(0);
    }

    public void restart() {
        start();
    }

    void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", boundPort), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }));
        server.createContext("/internal/generate/bible", ex -> respondSync(ex, """
                {"worldSetting":"세계","theme":"테마","tone":"톤","characters":[
                  {"name":"한서윤","description":"설명","personality":"성격","relationships":"관계"}]}"""));
        server.createContext("/internal/generate/outline", this::outline);
        server.createContext("/internal/generate/review", this::review);
        server.createContext("/internal/guide/chat", ex -> respondSync(ex, """
                {"reply":"안내입니다.","actions":[{"label":"Codicomperchat으로 이동","kind":"collab","target":"codicomperchat"},
                  {"label":"이슈 보기","kind":"route","target":"/issues"}]}"""));
        server.createContext("/internal/generate/chapter", this::chapter);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void reset() {
        chapterMode = ChapterMode.OK;
        failSync = false;
        chapterDelayMillis = 0;
        syncCalls.set(0);
        chapterRequests.clear();
        reviewFindsHighIssue = false;
        failReview = false;
        reviewRequests.clear();
    }

    public void reviewFindsHighIssue(boolean value) {
        this.reviewFindsHighIssue = value;
    }

    public void failReview(boolean value) {
        this.failReview = value;
    }

    private void review(HttpExchange ex) throws IOException {
        reviewRequests.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (failReview) {
            ex.sendResponseHeaders(500, -1);
            ex.close();
            return;
        }
        String issues = reviewFindsHighIssue ? "{\"type\":\"timeline\",\"description\":\"수정본 어긋남\",\"severity\":\"high\"}" : "";
        byte[] bytes = ("{\"summary\":\"수정본 요약\",\"report\":{\"issues\":[" + issues + "],\"score\":"
                + (reviewFindsHighIssue ? 70 : 100) + "}}").getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public void chapterMode(ChapterMode mode) {
        this.chapterMode = mode;
    }

    public int syncCalls() {
        return syncCalls.get();
    }

    public void chapterDelayMillis(long millis) {
        this.chapterDelayMillis = millis;
    }

    public void failSync(boolean fail) {
        this.failSync = fail;
    }

    private void outline(HttpExchange ex) throws IOException {
        String body = read(ex);
        int count = Integer.parseInt(body.replaceAll(".*\"chapterCount\":(\\d+).*", "$1"));
        StringBuilder chapters = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) {
                chapters.append(',');
            }
            chapters.append("""
                    {"order":%d,"title":"%d장","beats":["사건"],"characters":["한서윤"],"goals":"목표","continuityPoints":["복선"]}"""
                    .formatted(i, i));
        }
        respondSync(ex, "{\"chapters\":[" + chapters + "]}");
    }

    private void chapter(HttpExchange ex) throws IOException {
        String body = read(ex);
        chapterRequests.add(body);
        if (chapterDelayMillis > 0) {
            try {
                Thread.sleep(chapterDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (chapterMode == ChapterMode.HTTP_500) {
            ex.sendResponseHeaders(500, -1);
            ex.close();
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "text/event-stream");
        ex.sendResponseHeaders(200, 0);
        String sse = "event: token\ndata: 첫 문단\n\nevent: token\ndata:  이어서\n\n";
        sse += chapterMode == ChapterMode.SSE_ERROR
                ? "event: error\ndata: 생성 실패\n\n"
                : """
                event: done
                data: {"title":"제목","content":"첫 문단 이어서","summary":"요약","report":{"issues":[{"type":"timeline","description":"어긋남","severity":"high"}],"score":70}}

                """;
        ex.getResponseBody().write(sse.getBytes(StandardCharsets.UTF_8));
        ex.close();
    }

    private void respondSync(HttpExchange ex, String json) throws IOException {
        read(ex);
        syncCalls.incrementAndGet();
        if (failSync) {
            ex.sendResponseHeaders(500, -1);
            ex.close();
            return;
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static String read(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
