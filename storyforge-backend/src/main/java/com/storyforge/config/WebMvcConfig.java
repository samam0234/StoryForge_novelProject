package com.storyforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OwnershipInterceptor ownershipInterceptor;

    public WebMvcConfig(OwnershipInterceptor ownershipInterceptor) {
        this.ownershipInterceptor = ownershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ownershipInterceptor).addPathPatterns("/api/v1/**");
    }
}
