package com.example.be.global.exception;

import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Objects;

/**
 * 검증 실패 시 어떤 필드가 왜 틀렸는지 알려주는 상세 정보.
 * 실패 응답의 {@code data} 배열에 담긴다.
 *
 * @param field         문제가 된 필드명
 * @param rejectedValue 거부된 입력값
 * @param reason        실패 사유
 */
public record ValidationError(
        String field,
        String rejectedValue,
        String reason
) {

    public static ValidationError of(FieldError fieldError) {
        return new ValidationError(
                fieldError.getField(),
                Objects.toString(fieldError.getRejectedValue(), null),
                fieldError.getDefaultMessage()
        );
    }

    public static List<ValidationError> from(List<FieldError> fieldErrors) {
        return fieldErrors.stream().map(ValidationError::of).toList();
    }
}
