package com.example.be.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 예외.
 *
 * <p>서비스 계층에서 이 예외만 던지면 {@link GlobalExceptionHandler} 가
 * {@link ErrorCode} 에 정의된 HTTP 상태와 메시지로 변환한다.
 * 컨트롤러나 서비스에서 직접 try-catch 로 응답을 만들지 말 것.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황에 맞는 메시지를 내려줄 때 사용한다. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
