package com.storyforge.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Set;

/**
 * JWT 인증 (기획서 §7.1, docs/plan/auth.md). 프로젝트 소유자 검사는 OwnershipInterceptor가 합니다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint problemEntryPoint,
                                            RateLimitProperties rateLimit, ObjectMapper objectMapper,
                                            @Value("${storyforge.client-ip.trusted-proxies:}") String trustedProxies) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 챕터 SSE는 비동기 재디스패치로 응답을 마칩니다. 인증은 최초 요청에서 끝났습니다.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/mail/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/mail/reset-request", "/api/v1/auth/mail/reset", "/api/v1/auth/mail/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/llm-notice").permitAll()
                        // 커뮤니티 읽기는 공개입니다 (공유 링크를 로그인 없이 열 수 있어야 합니다). 내려받기는 로그인이 필요합니다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/works", "/api/v1/works/*", "/api/v1/works/*/chapters/*",
                                "/api/v1/works/*/comments", "/api/v1/works/*/activity", "/api/v1/works/*/shares",
                                "/api/v1/works/*/network", "/api/v1/posts", "/api/v1/posts/*", "/api/v1/posts/*/comments",
                                "/api/v1/users/*", "/api/v1/users/*/works", "/api/v1/users/*/contributions",
                                "/api/v1/users/*/activity", "/api/v1/users/*/achievements", "/api/v1/issues",
                                "/api/v1/media/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/shares/*/visit").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problemEntryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(problemEntryPoint))
                // 사용자별로 세므로 토큰 인증 뒤에 둡니다. 빈으로 등록하면 서블릿 필터로 한 번 더 걸립니다.
                .addFilterAfter(new RateLimitFilter(rateLimit, objectMapper, Clock.systemUTC(),
                                trusted(trustedProxies)),
                        BearerTokenAuthenticationFilter.class)
                .build();
    }

    /** 토큰 없음·만료를 다른 오류와 같은 ProblemDetail로 돌려줍니다. */
    @Bean
    AuthenticationEntryPoint problemEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(problem));
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${storyforge.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static Set<String> trusted(String raw) {
        return ClientAddresses.parse(raw);
    }
}
