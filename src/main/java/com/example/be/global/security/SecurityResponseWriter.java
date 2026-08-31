package com.example.be.global.security;

import com.example.be.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 필터 단계에서 발생한 인증/인가 실패를 ApiResponse 포맷의 JSON 으로 내려준다.
 *
 * <p>이 지점은 Spring MVC 바깥이라 {@code GlobalExceptionHandler} 가 개입하지 못한다.
 * 그렇다고 응답 포맷이 달라지면 프론트가 두 가지 형태를 처리해야 하므로 여기서 직접 만든다.
 *
 * <p>ObjectMapper 를 주입하지 않고 문자열로 조립하는 이유: Spring Boot 4 는 Jackson 3
 * ({@code tools.jackson.*}) 를 쓰는데, 필터 계층에서 매퍼 빈 타입에 의존할 이유가 없다.
 * 내려보내는 값이 전부 우리가 정의한 상수(ErrorCode)라 이스케이프가 필요한 입력이 섞이지 않는다.
 */
final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"success":false,"data":null,"message":"%s","code":"%s"}"""
                .formatted(errorCode.getMessage(), errorCode.getCode()));
    }
}
