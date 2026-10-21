package com.storyforge.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "storyforge.ai")
public record AiServiceProperties(String baseUrl, Duration timeout) {
}
