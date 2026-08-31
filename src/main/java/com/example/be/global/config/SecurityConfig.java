package com.example.be.global.config;

import com.example.be.global.security.JwtAccessDeniedHandler;
import com.example.be.global.security.JwtAuthenticationEntryPoint;
import com.example.be.global.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정.
 *
 * <p>JWT 검증은 직접 만든 필터가 아니라 Spring Security 의 리소스 서버 지원이 처리한다.
 * ({@code oauth2ResourceServer().jwt()} → BearerTokenAuthenticationFilter → JwtDecoder)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 인증 없이 접근 가능한 경로. */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반 무상태 API 이므로 CSRF 토큰이 필요 없다.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // CORS preflight 는 Authorization 헤더를 달고 오지 않는다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // 관리자 계정 생성/삭제는 ADMIN 만.
                        // 나머지 관리자 API 는 "본인 또는 ADMIN" 규칙이라 서비스 계층에서 검사한다.
                        .requestMatchers(HttpMethod.POST, "/api/managers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/managers/**").hasRole("ADMIN")

                        // 공정·구역은 현장 구조를 정의하는 마스터 데이터다.
                        // 조회는 모든 관리자가, 변경은 ADMIN 만 할 수 있다.
                        .requestMatchers(HttpMethod.POST, "/api/processes", "/api/zones").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/processes/**", "/api/zones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/processes/**", "/api/zones/**").hasRole("ADMIN")

                        .anyRequest().authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // 리소스 서버 바깥(예: 인가 실패)에서 발생하는 경우까지 같은 포맷으로 내려준다.
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));

        return http.build();
    }

    /**
     * 토큰의 {@code role} 클레임("ADMIN")을 Spring Security 권한("ROLE_ADMIN")으로 변환한다.
     * 이 매핑이 있어야 {@code hasRole("ADMIN")} 과 {@code @PreAuthorize} 가 동작한다.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(TokenProvider.CLAIM_ROLE);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /** 관리자 비밀번호 해싱에 사용한다. 평문 저장 금지. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
