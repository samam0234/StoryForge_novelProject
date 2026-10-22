package com.storyforge.config;

import com.storyforge.client.AiServiceProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.Objects;

@Configuration
public class WebClientConfig {

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
