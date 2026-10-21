package com.storyforge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI storyforgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("StoryForge API")
                .version("v1")
                .description("StoryForge Orchestrator 외부 API (기획서 §11)"));
    }
}
