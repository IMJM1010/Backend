package com.example.be.global.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT 서명/검증 설정. HMAC-SHA256(HS256) 대칭키를 사용한다.
 *
 * <p>{@code jwt.secret} 은 비밀값이므로 저장소에 커밋하지 않는다.
 * 로컬은 application-local.properties, 운영은 환경변수 {@code JWT_SECRET} 으로 주입한다.
 */
@Configuration
public class JwtConfig {

    /** HS256 의 최소 키 길이. 이보다 짧으면 Nimbus 가 서명 시점에 예외를 던진다. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public JwtConfig(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            // 기동 시점에 바로 알려준다. 런타임에 첫 로그인에서 터지면 원인 찾기가 어렵다.
            throw new IllegalStateException(
                    "jwt.secret 은 최소 %d바이트여야 합니다. 현재 %d바이트."
                            .formatted(MIN_SECRET_BYTES, keyBytes.length));
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
