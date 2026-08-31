package com.example.be.domain.worker.entity;

/**
 * 작업자 안전 상태. 생체 데이터와 환경 센서 판정 결과가 이 값으로 반영된다.
 *
 * <ul>
 *   <li>{@link #NORMAL} — 이상 없음</li>
 *   <li>{@link #CAUTION} — 주의. 임계값 근접</li>
 *   <li>{@link #DANGER} — 위험. 임계값 초과</li>
 *   <li>{@link #EMERGENCY} — 긴급. 즉시 조치 필요</li>
 * </ul>
 */
public enum SafetyStatus {
    NORMAL,
    CAUTION,
    DANGER,
    EMERGENCY
}
