package com.storyforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OwnershipInterceptor ownershipInterceptor;
    private final AccountGuardInterceptor accountGuard;

    public WebMvcConfig(OwnershipInterceptor ownershipInterceptor, AccountGuardInterceptor accountGuard) {
        this.accountGuard = accountGuard;
        this.ownershipInterceptor = ownershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accountGuard).addPathPatterns("/api/v1/**");
        registry.addInterceptor(ownershipInterceptor).addPathPatterns("/api/v1/**");
    }
}
