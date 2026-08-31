package com.example.be.domain.wearabledevice.entity;

/**
 * 디바이스 연결 상태.
 *
 * <p>{@link #RETIRED} 는 ERD 에 없는 확장 값이다. 폐기·분실 디바이스를 실제로 삭제하면
 * 그 디바이스를 참조하는 생체 기록 수만 건이 고아가 되므로, 삭제 대신 이 상태로 표시한다.
 */
public enum ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    ERROR,
    /** 폐기 또는 분실. 더 이상 사용하지 않으며 작업자 배정도 해제된다. */
    RETIRED;

    public boolean isRetired() {
        return this == RETIRED;
    }
}
