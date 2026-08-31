package com.example.be.global.security;

import com.example.be.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 모자란 요청에 403 을 내려준다.
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("권한 부족: {} {} - {}",
                request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
        SecurityResponseWriter.write(response, ErrorCode.ACCESS_DENIED);
    }
}
