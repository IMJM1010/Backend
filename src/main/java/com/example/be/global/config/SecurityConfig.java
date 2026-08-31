package com.example.be.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정.
 *
 * <p><b>⚠️ 현재는 임시 설정이다.</b> spring-boot-starter-security 가 클래스패스에 있으면
 * 기본적으로 모든 요청이 잠기기 때문에, JWT 인증을 붙이기 전까지 개발이 막히지 않도록
 * 전체 허용해 둔 상태다.
 *
 * <p>개발 2단계(인증)에서 아래를 적용하며 이 주석을 지울 것:
 * <ul>
 *   <li>JwtAuthenticationFilter 를 UsernamePasswordAuthenticationFilter 앞에 등록</li>
 *   <li>permitAll 대상을 /api/auth/login, /api/auth/refresh, Swagger 경로로 한정</li>
 *   <li>나머지는 authenticated(), 관리자 전용 경로는 hasRole("ADMIN")</li>
 *   <li>AuthenticationEntryPoint / AccessDeniedHandler 를 등록해
 *       필터 단계 인증·인가 실패도 ApiResponse 포맷으로 내려줄 것</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

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
                // TODO(2단계): 아래 permitAll 을 실제 인가 규칙으로 교체할 것
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * 관리자 비밀번호 해싱에 사용한다. 평문 저장 금지.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
