package com.example.be.global.exception;

import com.example.be.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 예외 처리기.
 *
 * <p>모든 예외는 여기서 {@link ApiResponse} 형태로 변환된다.
 * 컨트롤러나 서비스에서 try-catch 로 응답을 직접 만들지 말 것.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 규칙 위반. 의도된 실패이므로 스택 트레이스는 남기지 않는다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    /** {@code @Valid} 로 검증한 요청 본문이 실패한 경우. 어떤 필드가 왜 틀렸는지 함께 내려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {

        List<ValidationError> errors = ValidationError.from(e.getBindingResult().getFieldErrors());
        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT_VALUE,
                        ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                        errors));
    }

    /** {@code @Validated} 가 붙은 파라미터({@code @RequestParam}, {@code @PathVariable}) 검증 실패. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /** 파라미터 타입 불일치. 예: /api/workers/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "'%s' 값의 타입이 올바르지 않습니다.".formatted(e.getName());
        log.warn("Type mismatch: name={}, value={}", e.getName(), e.getValue());
        return ResponseEntity
                .status(ErrorCode.INVALID_TYPE_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_TYPE_VALUE, message));
    }

    /** 필수 쿼리 파라미터 누락. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "필수 파라미터 '%s' 가 누락되었습니다.".formatted(e.getParameterName());
        log.warn("Missing parameter: {}", e.getParameterName());
        return ResponseEntity
                .status(ErrorCode.MISSING_PARAMETER.getStatus())
                .body(ApiResponse.error(ErrorCode.MISSING_PARAMETER, message));
    }

    /** 요청 본문이 JSON 으로 파싱되지 않는 경우. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.MALFORMED_REQUEST_BODY.getStatus())
                .body(ApiResponse.error(ErrorCode.MALFORMED_REQUEST_BODY));
    }

    /** 지원하지 않는 HTTP 메서드. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMethod());
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /** 매핑되지 않은 경로. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("No resource found: {}", e.getResourcePath());
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /**
     * 메서드 보안(@PreAuthorize) 에서 걸린 권한 부족.
     * 필터 단계에서 발생하는 인증/인가 실패는 SecurityConfig 의
     * AuthenticationEntryPoint / AccessDeniedHandler 가 처리한다. (JWT 적용 시 추가)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED));
    }

    /**
     * DB 제약 위반. 유니크 키 중복 등이 서비스 계층 검증을 빠져나갔을 때의 마지막 방어선이다.
     * 어떤 제약인지 알 수 없으므로 일반 메시지를 내려주고, 구체적인 중복 검사는
     * 서비스에서 미리 하여 DUPLICATE_* 에러 코드로 처리할 것.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation", e);
        return ResponseEntity
                .status(ErrorCode.DATA_INTEGRITY_VIOLATION.getStatus())
                .body(ApiResponse.error(ErrorCode.DATA_INTEGRITY_VIOLATION));
    }

    /** 예상하지 못한 예외. 원인 파악을 위해 스택 트레이스를 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
