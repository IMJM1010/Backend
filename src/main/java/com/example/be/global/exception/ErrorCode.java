package com.example.be.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드.
 *
 * <p>enum 상수 이름이 그대로 응답의 {@code code} 값이 된다. (예: {@code WORKER_NOT_FOUND})
 * 프론트는 이 코드로 분기하므로, 이미 배포된 상수의 <b>이름을 바꾸면 프론트가 깨진다</b>.
 * 이름 변경 대신 새 상수를 추가하고 기존 것을 deprecated 처리할 것.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* ---------- 공통 ---------- */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "요청 값의 타입이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "데이터 무결성 제약에 위배됩니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    /* ---------- 인증 / 인가 ---------- */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /* ---------- 관리자 ---------- */
    MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 관리자입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 아이디입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),

    /* ---------- 공정 ---------- */
    PROCESS_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공정입니다."),
    PROCESS_HAS_ZONES(HttpStatus.CONFLICT, "하위 구역이 존재하여 삭제할 수 없습니다."),

    /* ---------- 구역 ---------- */
    ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 구역입니다."),
    DUPLICATE_ZONE_CODE(HttpStatus.CONFLICT, "이미 사용 중인 구역 코드입니다."),
    ZONE_HAS_WORKERS(HttpStatus.CONFLICT, "소속 작업자가 존재하여 삭제할 수 없습니다."),

    /* ---------- 작업자 ---------- */
    WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 작업자입니다."),
    DUPLICATE_EMPLOYEE_NO(HttpStatus.CONFLICT, "이미 등록된 사번입니다."),
    WORKER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "퇴사 처리된 작업자입니다."),

    /* ---------- 근태 ---------- */
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 근태 기록입니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "이미 출근 처리된 작업자입니다."),
    NOT_CHECKED_IN(HttpStatus.BAD_REQUEST, "출근 기록이 없어 퇴근 처리할 수 없습니다."),
    ALREADY_CHECKED_OUT(HttpStatus.CONFLICT, "이미 퇴근 처리되었습니다."),

    /* ---------- 웨어러블 디바이스 ---------- */
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 디바이스입니다."),
    DUPLICATE_SERIAL_NO(HttpStatus.CONFLICT, "이미 등록된 시리얼 번호입니다."),
    DEVICE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 다른 작업자에게 배정된 디바이스입니다."),

    /* ---------- 생체 기록 ---------- */
    VITAL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 생체 기록입니다."),

    /* ---------- 환경 센서 / 기록 ---------- */
    SENSOR_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 환경 센서입니다."),
    INVALID_THRESHOLD_RANGE(HttpStatus.BAD_REQUEST, "임계값 하한이 상한보다 클 수 없습니다."),
    ENV_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 환경 기록입니다."),

    /* ---------- 알림 ---------- */
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    ALERT_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확인 처리된 알림입니다."),
    ALERT_WORKER_ALREADY_MAPPED(HttpStatus.CONFLICT, "이미 알림 대상으로 등록된 작업자입니다."),
    ALERT_WORKER_NOT_MAPPED(HttpStatus.NOT_FOUND, "알림 대상으로 등록되지 않은 작업자입니다."),

    /* ---------- 사고 ---------- */
    INCIDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사고입니다."),
    INCIDENT_ALREADY_RESOLVED(HttpStatus.CONFLICT, "이미 처리 완료된 사고입니다."),

    /* ---------- 긴급 조치 ---------- */
    EMERGENCY_ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 긴급 조치 로그입니다."),
    EMERGENCY_ACTION_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "감사 목적상 긴급 조치 로그는 삭제할 수 없습니다."),
    PROCESS_ID_REQUIRED_FOR_STOP(HttpStatus.BAD_REQUEST, "공정 중단 조치에는 공정 식별자가 필요합니다."),

    /* ---------- AI 인사이트 ---------- */
    AI_INSIGHT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 AI 인사이트입니다."),
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 인사이트 생성에 실패했습니다."),

    /* ---------- 대시보드 위젯 ---------- */
    WIDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 위젯입니다."),
    DUPLICATE_WIDGET_TYPE(HttpStatus.CONFLICT, "이미 추가된 위젯 종류입니다.");

    private final HttpStatus status;
    private final String message;

    /** 응답의 {@code code} 값. enum 상수 이름을 그대로 사용한다. */
    public String getCode() {
        return name();
    }
}
