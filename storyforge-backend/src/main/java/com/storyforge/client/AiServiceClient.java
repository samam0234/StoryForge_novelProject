package com.storyforge.client;

import com.storyforge.client.AiDtos.*;
import com.storyforge.exception.AiServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Objects;

/**
 * 내부망 FastAPI AI Service 호출. 백엔드에서 FastAPI로 가는 호출은 모두 여기를 거칩니다.
 */
@Component
public class AiServiceClient {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final Duration timeout;

    public AiServiceClient(WebClient aiWebClient, AiServiceProperties properties) {
        this.webClient = aiWebClient;
        this.timeout = properties.timeout();
    }

    public BibleGenerateResponse generateBible(BibleGenerateRequest request) {
        return post("/internal/generate/bible", request, BibleGenerateResponse.class);
    }

    public OutlineGenerateResponse generateOutline(OutlineGenerateRequest request) {
        return post("/internal/generate/outline", request, OutlineGenerateResponse.class);
    }

    /** 이벤트: token(본문 조각), done(ChapterGenerateResult JSON), error(메시지) */
    public Flux<ServerSentEvent<String>> generateChapter(ChapterContext context) {
        return webClient.post()
                .uri("/internal/generate/chapter")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(Objects.requireNonNull(context))
                .retrieve()
                .bodyToFlux(Objects.requireNonNull(SSE_TYPE))
                .timeout(timeout)
                .onErrorMap(e -> !(e instanceof AiServiceException),
                        e -> new AiServiceException("AI 서비스 챕터 생성 실패: " + e.getMessage(), e));
    }

    private <T> T post(String uri, Object body, Class<T> type) {
        try {
            T result = webClient.post()
                    .uri(Objects.requireNonNull(uri))
                    .bodyValue(Objects.requireNonNull(body))
                    .retrieve()
                    .bodyToMono(Objects.requireNonNull(type))
                    .block(timeout);
            if (result == null) {
                throw new AiServiceException("AI 서비스 응답이 비어 있습니다: " + uri, null);
            }
            return result;
        } catch (WebClientException | IllegalStateException e) {
            throw new AiServiceException("AI 서비스 호출 실패: " + uri + " (" + e.getMessage() + ")", e);
        }
    }
}
