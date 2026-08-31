package com.example.be.global.security;

import com.example.be.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청(토큰 없음 / 만료 / 위조)에 401 을 내려준다.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("인증 실패: {} {} - {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());
        SecurityResponseWriter.write(response, ErrorCode.UNAUTHORIZED);
    }
}
