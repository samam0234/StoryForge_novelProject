package com.storyforge.client;

import com.storyforge.client.AiDtos.*;
import com.storyforge.exception.AiServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;

/**
 * 내부망 FastAPI AI Service 호출. 백엔드에서 FastAPI로 가는 호출은 모두 여기를 거칩니다.
 * 타임아웃, 연결 실패 재시도(동기 호출 1회), Circuit Breaker를 적용합니다 (기획서 §15).
 */
@Component
public class AiServiceClient {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final Duration timeout;
    private final CircuitBreaker circuitBreaker;

    public AiServiceClient(WebClient aiWebClient, AiServiceProperties properties, CircuitBreaker aiCircuitBreaker) {
        this.webClient = aiWebClient;
        this.timeout = properties.timeout();
        this.circuitBreaker = aiCircuitBreaker;
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
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(e -> !(e instanceof AiServiceException),
                        e -> new AiServiceException("AI 서비스 챕터 생성 실패: " + e.getMessage(), e));
    }

    private <T> T post(String uri, Object body, Class<T> type) {
        T result;
        try {
            result = webClient.post()
                    .uri(Objects.requireNonNull(uri))
                    .bodyValue(Objects.requireNonNull(body))
                    .retrieve()
                    .bodyToMono(Objects.requireNonNull(type))
                    // 요청이 서버에 닿지 못한 연결 실패만 재시도합니다. 생성 비용이 드는 응답 오류는 재시도하지 않습니다.
                    .retryWhen(Retry.backoff(1, Duration.ofMillis(300))
                            .filter(WebClientRequestException.class::isInstance)
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .timeout(timeout)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .block();
        } catch (RuntimeException e) {
            // 응답 오류, 연결 실패, 타임아웃(block이 감싸서 던짐), 호출 차단을 모두 502로 전파합니다.
            throw new AiServiceException("AI 서비스 호출 실패: " + uri + " (" + e.getMessage() + ")", e);
        }
        if (result == null) {
            throw new AiServiceException("AI 서비스 응답이 비어 있습니다: " + uri, null);
        }
        return result;
    }
}
