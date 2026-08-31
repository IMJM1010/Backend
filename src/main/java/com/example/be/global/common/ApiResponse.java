package com.example.be.global.common;

import com.example.be.global.exception.ErrorCode;

/**
 * 모든 API 의 공통 응답 포맷.
 *
 * <pre>
 * 성공: { "success": true,  "data": {...}, "message": null, "code": null }
 * 실패: { "success": false, "data": null,  "message": "존재하지 않는 작업자입니다.", "code": "WORKER_NOT_FOUND" }
 * </pre>
 *
 * <p>주의: springdoc 의 {@code io.swagger.v3.oas.annotations.responses.ApiResponse} 와 이름이 같다.
 * 컨트롤러에서 Swagger 응답 애노테이션을 쓸 때는 정규화된 이름을 사용하거나 {@code @Operation} 만 사용할 것.
 *
 * @param success 성공 여부
 * @param data    응답 본문 (실패 시 null, 단 검증 실패는 필드별 상세가 담긴다)
 * @param message 사용자에게 보여줄 메시지 (성공 시 null)
 * @param code    에러 식별 코드. 프론트 분기용 (성공 시 null)
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String code
) {

    /* ---------- 성공 ---------- */

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /**
     * 반환할 데이터가 없는 성공 응답. (상태 변경, 삭제 등)
     *
     * <p>이름이 {@code success()} 가 아닌 이유: {@code success} 가 레코드 컴포넌트라
     * 무인자 접근자 {@code success()} 가 자동 생성되어 같은 이름의 정적 메서드를 선언할 수 없다.
     */
    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, null, null, null);
    }

    /* ---------- 실패 ---------- */

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, errorCode.getMessage(), errorCode.getCode());
    }

    /** 에러 코드의 기본 메시지 대신 상황에 맞는 메시지를 내려줄 때 사용한다. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode.getCode());
    }

    /** 검증 실패처럼 실패 응답에도 상세 데이터를 실어야 할 때 사용한다. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, data, message, errorCode.getCode());
    }
}
