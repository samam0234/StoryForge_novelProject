package com.storyforge.config;

import com.storyforge.client.AiServiceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Objects;

@Configuration
public class WebClientConfig {

    /** AI 서비스가 계속 실패하면 30초간 호출을 막아 빠르게 실패시킵니다 (기획서 §15). */
    @Bean
    CircuitBreaker aiCircuitBreaker() {
        return CircuitBreaker.of("ai-service", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }

    @Bean
    WebClient aiWebClient(WebClient.Builder builder, AiServiceProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(properties.timeout());
        return builder
                .baseUrl(Objects.requireNonNull(properties.baseUrl(), "storyforge.ai.base-url 설정이 필요합니다."))
                .clientConnector(new ReactorClientHttpConnector(Objects.requireNonNull(httpClient)))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }
}
